package com.gmmapowell.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.http.RemoteIO.Connection;

public class ConnectionHandler implements NIOActionable {
	enum Mode { START, HEADERS, BODY, WEBSOCKET };
	private final InlineServer inlineServer;
	private Mode mode = Mode.START;
	private boolean closeConnection;
	private final Connection conn;
	private final SocketChannel chan;
	private GPRequest request;
	private GPResponse response;
	private Date requestStart;
	private StringBuilder currentLine;
	private int rthr = 0;
	private ByteBuffer buffer;
	private GPFrame currentFrame = null;
	private final byte[] frameHeader = new byte[14];
	private int fhread = 0;
	private int fhmax;
	private int fbread;

	public ConnectionHandler(InlineServer inlineServer, Connection conn) throws IOException {
		InlineServer.logger.debug("Creating connection handler " + this + " for " + conn);
		this.inlineServer = inlineServer;
		this.conn = conn;
		this.chan = conn.getChannel();
		this.buffer = ByteBuffer.allocate(4000);
	}
	
	@Override
	public boolean ready(SelectableChannel channel) throws Exception {
		// The first thing to do is to read as much of the socket's contents as we have available
		InlineServer.logger.debug(this + ": " + buffer);
		int pos = buffer.position();
		int cnt = chan.read(buffer);
		int lim = buffer.position();
		if (cnt == -1) {
			InlineServer.logger.info("End of stream seen, closing " + this);
			chan.close();
			return false;
		}
		buffer.position(pos);
		buffer.limit(lim);
		if (!buffer.hasRemaining()) {
			InlineServer.logger.debug("Nothing remaining in buffer, asking for more " + this);
			buffer.clear();
			return true;
		}
		InlineServer.logger.debug(this + ": buffer = " + buffer + "; contents = " + new String(buffer.array(), buffer.position(), buffer.limit()));
		
		// Now figure out where we are
		if (mode == Mode.START || mode == Mode.HEADERS) {
			return readHeaders();
		} else if (mode == Mode.BODY){
			synchronized (this) {
				this.notify();
				InlineServer.logger.debug("In body mode");
				return false;
			}
		} else if (mode == Mode.WEBSOCKET) {
			readFrame();
			return true;
		} else
			throw new UtilException("Cannot handle " + mode);
	}

	public boolean readHeaders() throws Exception {
		if (mode == Mode.START) {
			// Initialize request state
			request = null;
			response = null;
			closeConnection = true;
			InlineServer.logger.debug(this+ ": Processing Incoming Request");
			requestStart = new Date();
			mode = Mode.HEADERS;
			currentLine = new StringBuilder();
		}
		while (true)
		{
			String s = readLine();
			if (s == null)
				return true;
			else if (s.trim().length() == 0) {
				mode = Mode.BODY;
				break;
			}
			InlineServer.logger.debug(this+ ": " +"Header - " + s);
			if (request == null)
				request = inlineServer.requestFor(this, s, chan);
			else
				request.addHeader(s);
		}
		request.endHeaders(buffer);
		InlineServer.logger.debug("Handling request for " + request.getRequestURI());
		String connhdr;
		{
			connhdr = request.getHeader("connection");
			if (connhdr == null)
				connhdr = "close";
			else if (connhdr.equalsIgnoreCase("keep-alive"))
				closeConnection = false;
			else if (connhdr.equalsIgnoreCase("upgrade"))
				closeConnection = false;
			else if (connhdr.equalsIgnoreCase("close"))
				closeConnection = true;
		}
		
		response = new GPResponse(request, chan, connhdr);
		
		if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
			response.setStatus(200);
			request.getInputStream().flush();
			sendResponse();
			return true;
		}

		// Test for web socket request
		{
			String upghdr = request.getHeader("upgrade");
			if (upghdr != null && (upghdr.equalsIgnoreCase("websocket")))
				response.setWebSocket(true);
		}
		
		RequestThread rt = new RequestThread(this, request, response);
		rt.setName("RequestThr#" + (++rthr));
		rt.start();
		InlineServer.logger.info("Launching request thread for " + request.getRequestURI() + " from " + this + " in " + rt.getName());
		return false;
	}

	private String readLine() throws IOException {
		while (true) {
			if (!buffer.hasRemaining()) {
				buffer.clear();
				return null;
			}
			char c = (char) buffer.get();
			if (c == '\n')
				break;
			else
				currentLine.append(c);
		}

		// only if we get a newline do we get here
		for (int i=currentLine.length();i>0;i--)
		{
			if (currentLine.charAt(i-1) != '\r' && currentLine.charAt(i-1) != '\n')
				break;
			currentLine.deleteCharAt(i-1);
		}
		String ret = currentLine.toString();
		currentLine = new StringBuilder();
		return ret;
	}

	public void wantMore(boolean isSync) {
		try {
			inlineServer.reregister(this, chan, isSync);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	void sendResponse() {
		if (response != null)
		{
			if (response.getStatus() == 0 && closeConnection)
				response.setStatus(200, "OK");
			response.commit();
			try
			{
				response.getWriter().flush();
				conn.doneSending();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void handleError(Exception e) {
		InlineServer.logger.error("Uncaught exception processing request", e);
		try {
			conn.errorSending();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void andFinally() {
		try {
			request.getInputStream().flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		inlineServer.requestTime(this, requestStart, new Date());
		if (response == null)
			closeConnection = true;
		else if (response.getHeader("Content-Length") != null && response.getHeader("Content-Length").equals("-1"))
			closeConnection = true;
		mode = Mode.START;
		request = null;
		response = null;
		requestStart = null;
		try
		{
			if (closeConnection) {
				InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Closing Connection");
				chan.close();
			} else {
				wantMore(false);
				readHeaders();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void useWebSocket() {
		mode = Mode.WEBSOCKET;
		wantMore(false);
		request.wshandler.onOpen(response);
		resetWSFraming();
		readFrame();
	}

	private void resetWSFraming() {
		currentFrame = null;
		fhmax = 6;
		fhread = 0;
		fbread = 0;
	}

	public boolean sendPing() {
		if (mode == Mode.WEBSOCKET) {
			try {
				response.writePingMessage();
				response.writePongMessage();
				return true;
			} catch (IOException ex) {
				response.close();
				return false;
			}
		} else
			return true;
	}
	
	private void readFrame() {
		while (true) {
			if (currentFrame == null) {
				while (fhread < fhmax) {
					if (!buffer.hasRemaining()) {
						buffer.clear();
						return;
					}
					byte b = buffer.get();
//					InlineServer.logger.info("Read byte " + fhread + ": " + (((int)b)&0xff));
					frameHeader[fhread++] = b; 
					if (fhread == 1) { // process first byte
						@SuppressWarnings("unused") // is this the final frame?
						boolean fin = (b&0x80) != 0;
						int rsv = (b&0x70);
						if (rsv != 0)
							throw new UtilException("RSV must be 0");
					} else if (fhread == 2) { // process length byte
						boolean isMasked = (b&0x80) != 0;
						if (!isMasked)
							throw new UtilException("Client must mask frames");
						int len = (b&0x7f);
						if (len == 126)
							fhmax+=2;
						else if (len == 127)
							fhmax+=8;
					}
				}
				
				// By the time we get here we will have one of these packets in frameHeader:
				// b1 b2 m1 m2 m3 m4
				// b1 b2 l1 l2 m1 m2 m3 m4
				// b1 b2 l1 l2 l3 l4 l5 l6 l7 l8 m1 m2 m3 m4
				// We can't actually allocate 8-byte long buffers, so we will only use l1/l2 or l5-l8
				int len;
				if (fhmax == 6)
					len = frameHeader[1] & 0x7f; // it is < 126 (the very, very normal case)
				else if (fhmax == 8)
					len = ((((int)frameHeader[2]) << 8) | (((int)frameHeader[3])&0xff)) & 0xffff; // 127-65535
				else { // 8-byte len
					len = 0;
					for (int i=6;i<=10;i++)
						len = (len << 8) | (((int)frameHeader[i])&0xff);
				}
				currentFrame = new GPFrame(frameHeader[0]&0xf, new byte[len]);
				fbread = 0;
			}
			
			while (fbread < currentFrame.data.length) {
				if (!buffer.hasRemaining()) {
					buffer.clear();
					return;
				}
				currentFrame.data[fbread] = (byte) (buffer.get() ^ frameHeader[fhmax-4+(fbread%4)]);
				fbread++;
			}
			
			// TODO: starting a separate thread for each request could be (a) wasteful and (b) break an implicit ordering guarantee
			// Should we instead queue these frames on a single (longer, but not infinitely, lived) FrameThread?
			FrameThread thr = new FrameThread(this, request.wshandler, currentFrame);
			thr.start();
			resetWSFraming();
		}
	}

	public void closeChan() throws IOException {
		chan.close();
		InlineServer.logger.info(Thread.currentThread().getName()+ ": " + "End of async stream");
	}
}
