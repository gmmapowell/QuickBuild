package com.gmmapowell.serialization;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gmmapowell.exceptions.UtilException;

public class SerializedControllerConnection extends Thread {
	private final SerializedController parent;
	private final Logger logger;
	private final Socket conn;
	private final Class<?>[] classes; 
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	
	public SerializedControllerConnection(String loggerName, SerializedController parent, Socket conn, Class<?>[] classes) throws IOException {
		this.parent = parent;
		logger = Logger.getLogger(loggerName+"*");
		logger.info("Connection received on " + conn.getLocalPort());
		this.conn = conn;
		this.classes = classes;
	}

	@Override
	public void run() {
		try
		{
			while (true) {
				synchronized (this)
				{
					try
					{
						if (ois == null)
							ois = new ObjectInputStream(conn.getInputStream());
						Serializable ret = handleRequest();
						logger.info("Finished handling request");
						if (ret != null) {
							logger.info("Sending response");
							if (oos == null)
								oos = new ObjectOutputStream(conn.getOutputStream());
							oos.writeObject(ret);
							oos.flush();
							logger.info("Response Complete");
						}
					}
					catch (EOFException endOfStream)
					{
						break;
					}
					
				}
			}
		}
		catch (Throwable t)
		{
			logger.log(Level.SEVERE, "Exception encountered", t);
		}
		try {
			conn.close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to close connection", e);
		}
		parent.closingConnection(this);
		logger.info("Thread finishing");
	}

	private Serializable handleRequest() throws IOException, ClassNotFoundException {
		ControlRequest cr = (ControlRequest)ois.readObject();
		logger.info("Received request " + cr);
		boolean isok = false;
		for (Class<?> clz : classes)
			isok |= clz.isInstance(cr);
		if (!isok)
			throw new UtilException("Invalid request type: " + cr.getClass());
		Serializable ret = cr.execute(this);
		if ((ret != null) != cr.waitForResponse())
			throw new UtilException("Expect Response = " + cr.waitForResponse() + "; Response = " + ret);
		return ret;
	}

	public SerializedController getParent() {
		return parent;
	}
}
