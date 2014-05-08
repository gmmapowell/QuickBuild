package com.gmmapowell.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

import com.gmmapowell.exceptions.NotImplementedException;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.serialization.Endpoint;
import com.gmmapowell.sync.SyncUtils;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;

public interface RemoteIO {
	void init() throws Exception;

	void announce(List<NotifyOnServerReady> interestedParties);

	void select() throws Exception;
	
	void reregister(ConnectionHandler connectionHandler, SocketChannel chan, boolean isSync) throws ClosedChannelException;

	Connection accept() throws Exception;
	
	void doneSending(Connection conn) throws Exception;

	void errorSending(Connection connection) throws Exception;

	String getEndpoint();

	void close() throws Exception;

	public class Connection {
		private final RemoteIO parent;
		private final SocketChannel chan;
		private InputStream is;
		private OutputStream os;
		private boolean done;

		public Connection(RemoteIO parent, InputStream is, OutputStream os) {
			this.parent = parent;
			this.chan = null;
			this.is = is;
			this.os = os;
		}

		public Connection(RemoteIO parent, SocketChannel chan) {
			this.parent = parent;
			this.chan = chan;
			this.is = null;
			this.os = null;
		}

		public final InputStream getInputStream() {
			if (is == null)
				is = Channels.newInputStream(chan);
			return is;
		}

		public final OutputStream getOutputStream() {
			if (os == null)
				os = Channels.newOutputStream(chan);
			return os;
		}
		
		public final SocketChannel getChannel() {
			return chan;
		}
		
		public void doneSending() throws Exception {
			done = true;
			parent.doneSending(this);
		}

		public void errorSending() throws Exception {
			if (!done)
				parent.errorSending(this);
		}
	}

	class UsingSocket implements RemoteIO, NIOActionable {
		private final int port;
		private int timeout = 10;
		private final InlineServer server;
		private ServerSocketChannel s;
		private Selector sel;

		public UsingSocket(InlineServer server, int port) {
			this.server = server;
			this.port = port;
		}

		@Override
		public void init() throws Exception {
			s = ServerSocketChannel.open();
			s.configureBlocking(false);
			s.socket().bind(new InetSocketAddress(port));
			sel = Selector.open();
			SelectionKey sk = s.register(sel, SelectionKey.OP_ACCEPT);
			sk.attach(this);
			InlineServer.logger.info("Listening on port " + s.socket().getInetAddress());
		}
		
		@Override
		public void announce(List<NotifyOnServerReady> interestedParties) {
			try {
				Endpoint addr = new Endpoint(s);
				if (interestedParties != null)
					for (NotifyOnServerReady nosr : interestedParties)
						nosr.serverReady(server, addr);
			} catch (Exception ex) {
				throw UtilException.wrap(ex);
			}
		}

		@Override
		public Connection accept() throws Exception {
			int cnt = sel.select(timeout);
			if (cnt == 0) {
				timeout = Math.min(timeout*2, 2000);
				return null;
			}
			timeout = 10;
			Iterator<SelectionKey> it = sel.selectedKeys().iterator();
			it.next();
			it.remove();
			return new Connection(this, s.accept());
		}

		@Override
		public void select() throws Exception {
			int cnt = sel.select(timeout);
			if (cnt == 0) {
				timeout = Math.min(timeout*2, 2000);
				return;
			}
			timeout = 10;
			Iterator<SelectionKey> it = sel.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey sk = it.next();
				it.remove();
				try {
					NIOActionable handler = (NIOActionable)sk.attachment();
					boolean wantMore = handler.ready(sk.channel());
					if (!wantMore) {
						InlineServer.logger.debug("Cancelling " + handler);
						sk.cancel();
					}
				} catch (Exception ex) {
					sk.cancel();
					sk.channel().close();
				}
			}
		}

		// When this comes back to us, it's the accept() that's gone off
		@Override
		public boolean ready(SelectableChannel channel) throws Exception {
			SocketChannel newConn = s.accept();
			newConn.configureBlocking(false);
			ConnectionHandler handler = new ConnectionHandler(server, new Connection(this, newConn));
			server.handlers.add(handler);
			newConn.register(sel, SelectionKey.OP_READ, handler);
			sel.wakeup();
			return true;
		}

		@Override
		public void reregister(ConnectionHandler connectionHandler, SocketChannel chan, boolean isSync) throws ClosedChannelException {
			synchronized (connectionHandler) {
				while (true) {
					try {
						sel.wakeup();
						chan.register(sel, SelectionKey.OP_READ, connectionHandler);
						break;
					} catch (CancelledKeyException ex) {
						// We were just too quick ... need to give the main thread a go
						SyncUtils.sleep(1);
					}
				}
				if (isSync) {
					SyncUtils.waitFor(connectionHandler, 0);
				}
			}
		}

		@Override
		public void doneSending(Connection conn) {
			// Nothing to do
		}

		@Override
		public void errorSending(Connection connection) throws Exception {
			// Nothing to do
		}

		@Override
		public void close() throws Exception {
			if (s != null)
				s.close();
			if (sel != null)
				sel.close();
		}

		@Override
		public String getEndpoint() {
			try {
				return new Endpoint(s).toString();
			} catch (IOException ex) {
				throw UtilException.wrap(ex);
			}
		}

	}
	
	public class AMQPConnection extends Connection {
		private final String replyTo;
		private final BasicProperties replyProps;
		private final long tag;

		public AMQPConnection(UsingAMQP parent, ByteArrayInputStream bais, ByteArrayOutputStream baos, String replyTo, long tag, BasicProperties replyProps) {
			super(parent, bais, baos);
			this.replyTo = replyTo;
			this.tag = tag;
			this.replyProps = replyProps;
		}

	}

	class UsingAMQP implements RemoteIO {

		private final String amqpUri;
		private Channel channel;
		private QueueingConsumer consumer;
		private com.rabbitmq.client.Connection connection;
		private final InlineServer server;

		public UsingAMQP(InlineServer inlineServer, String amqpUri) {
			this.server = inlineServer;
			this.amqpUri = amqpUri;
		}

		@Override
		public void init() throws Exception {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri(amqpUri);
			connection = factory.newConnection();
			channel = connection.createChannel();

			channel.queueDeclare("ziniki", false, false, false, null);
			channel.basicQos(1);

			consumer = new QueueingConsumer(channel);
			channel.basicConsume("ziniki", false, consumer);
		}

		@Override
		public void announce(List<NotifyOnServerReady> interestedParties) {
			for (NotifyOnServerReady nosr : interestedParties)
				nosr.serverReady(server, null);
		}

		@Override
		public Connection accept() throws Exception {
			Delivery req = null;
			try
			{
				req = consumer.nextDelivery();
				BasicProperties props = req.getProperties();
				BasicProperties replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();
				ByteArrayInputStream bais = new ByteArrayInputStream(req.getBody());
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				return new AMQPConnection(this, bais, baos, props.getReplyTo(), req.getEnvelope().getDeliveryTag(), replyProps);
			}
			catch (Exception ex) {
				// If something goes wrong here, make sure to ack it to prevent looping ...
				InlineServer.logger.error("Encountered error reading from AMQP", ex);
				if (req != null)
					channel.basicAck(req.getEnvelope().getDeliveryTag(), false);
				return null;
			}
		}

		@Override
		public void select() throws Exception {
			throw new NotImplementedException();
		}

		@Override
		public void reregister(ConnectionHandler connectionHandler, SocketChannel chan, boolean isSync) throws ClosedChannelException {
			throw new NotImplementedException();
		}

		@Override
		public void doneSending(Connection c) throws Exception {
			AMQPConnection conn = (AMQPConnection) c;
			try {
				if (conn.replyTo != null)
					channel.basicPublish("", conn.replyTo, conn.replyProps, ((ByteArrayOutputStream)conn.getOutputStream()).toByteArray());
			}
			finally {
				try
				{
					channel.basicAck(conn.tag, false);
				}
				catch (AlreadyClosedException ex) {
					InlineServer.logger.info("Tried to ack, but channel had gone away");
				}
			}
		}

		@Override
		public void errorSending(Connection c) throws Exception {
			AMQPConnection conn = (AMQPConnection) c;
			channel.basicAck(conn.tag, false);
		}

		@Override
		public void close() throws Exception {
			InlineServer.logger.info("Closing connection");
			connection.close();
		}

		@Override
		public String getEndpoint() {
			return amqpUri;
		}
	}
}
