package com.gmmapowell.serialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.gmmapowell.exceptions.UtilException;

public class EndpointConnection {

	private ObjectInputStream ois;
	private final ObjectOutputStream oos;
	private final Socket socket;

	public EndpointConnection(Socket socket, ObjectOutputStream oos) {
		this.socket = socket;
		this.ois = null;
		this.oos = oos;
	}

	public void send(ControlRequest msg) {
		try {
			oos.writeObject(msg);
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	public ObjectInputStream getOIS() {
		if (ois != null)
			return ois;
		try
		{
			ois = new ObjectInputStream(socket.getInputStream());
			return ois;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public void close() {
		try
		{
			socket.close();
		}
		catch (IOException ex)
		{
			;
		}
	}
}
