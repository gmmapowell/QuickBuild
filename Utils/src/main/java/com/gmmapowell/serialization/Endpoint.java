package com.gmmapowell.serialization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import com.gmmapowell.exceptions.UtilException;

@SuppressWarnings("serial")
public class Endpoint implements Serializable {
	private static final Logger logger = Logger.getLogger("Endpoint");
	private final String host;
	private final int port;

	public static Endpoint forPort(int port)
	{
		return new Endpoint(getLocalHostAddr(), port);
	}
	
	public Endpoint(InetAddress addr, int port) {
		String host = addr.getHostAddress();
		if (host.equals("0.0.0.0"))
			host = getLocalHostAddr();
		this.host = host;
		this.port = port;
	}

	public Endpoint(ServerSocket s) {
		this(s.getInetAddress(), s.getLocalPort());
	}
	
	public Endpoint(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	private static String getLocalHostAddr() {
		try {
			String host = null;
			InetAddress in = InetAddress.getLocalHost();
			logger.fine("Considering addresses for host " + in.getHostName() + ": " + in.getHostAddress());
			InetAddress[] all = InetAddress.getAllByName(in.getHostName());
			for (int i=0;i<all.length;i++)
			{
				logger.finer("resolving local address:" + all[i] + " " + all[i].isSiteLocalAddress() + " " + all[i].isLoopbackAddress());
				if (!all[i].isLoopbackAddress())
				{
					host = all[i].getHostAddress();
					break;
				}
			}
			if (host == null)
			{
				logger.severe("Could not find any local site address, using default: " + in.getHostAddress());
				host = in.getHostAddress();
			}
			logger.info("Identifying local host as " + host);
			return host;
		} catch (UnknownHostException e) {
			throw UtilException.wrap(e);
		}
	}

	public static Endpoint parse(String s) {
		try {
			int idx = s.indexOf(":");
			if (idx < 1 || idx > s.length()-2)
				throw new UtilException("Invalid address: " + s);
			String address = s.substring(0, idx);
			InetAddress addr = InetAddress.getByName(address);
			int port = Integer.parseInt(s.substring(idx+1));
			if (port < 0 || port > 65535)
				throw new UtilException("Invalid port: " + port);
			return new Endpoint(addr, port);
		} catch (Exception e) {
			System.out.println("Failed to interpret address: " + s);
			if (e instanceof UtilException)
				System.out.println("  " + e.getMessage());
			else
				System.out.println("  " + e.toString());
			return null;
		}
	}

	@Override
	public String toString() {
		return host+":"+port;
	}

	public void checkExists() {
		try
		{
			Socket socket = new Socket(host, port);
			socket.close();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public EndpointConnection open() {
		try
		{
			Socket socket = new Socket(host, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			
			return new EndpointConnection(socket, oos); 
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public Object tell(ControlRequest msg) {
		EndpointConnection open = open();
		open.send(msg);

		try
		{
			if (!msg.waitForResponse())
			{
				Thread.sleep(10);
				return null;
			}
			ObjectInputStream ois = open.getOIS();
			return ois.readObject();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
		finally
		{
			open.close();
		}
	}

	public void send(String string) {
		try {
			Socket conn = new Socket(host, port);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			bw.write(string);
			bw.flush();
			conn.close();
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Endpoint))
			return false;
		Endpoint other = (Endpoint) obj;
		return other.host.equals(host) && other.port == port;
	}
	
	@Override
	public int hashCode() {
		return host.hashCode() ^ port;
	}
}
