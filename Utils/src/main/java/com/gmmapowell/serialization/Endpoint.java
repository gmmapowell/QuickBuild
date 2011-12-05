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

import com.gmmapowell.exceptions.UtilException;

@SuppressWarnings("serial")
public class Endpoint implements Serializable {
	private final String host;
	private final int port;

	public Endpoint(InetAddress addr, int port) {
		String host = addr.getHostAddress();
		if (host.equals("0.0.0.0"))
			try {
				host = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		this.host = host;
		this.port = port;
	}

	public Endpoint(ServerSocket s) {
		this(s.getInetAddress(), s.getLocalPort());
	}
	
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
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
}
