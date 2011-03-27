package com.gmmapowell.system;

import java.io.InputStream;
import java.io.OutputStream;

import com.gmmapowell.exceptions.UtilException;

public class ThreadedStreamReader extends Thread {
	private final OutputStream writeTo;
	private InputStream readFrom;

	public ThreadedStreamReader() {
		this.writeTo = null;
	}

	public ThreadedStreamReader(OutputStream out) {
		this.writeTo = out;
	}

	@Override
	public void run() {
		try
		{
			byte[] bs = new byte[400];
			int cnt = 0;
			while ((cnt = readFrom.read(bs, 0, 400)) > 0)
				if (writeTo != null)
					writeTo.write(bs, 0, cnt);
			readFrom.close();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public void read(InputStream inputStream) {
		readFrom = inputStream;
		start();
	}
}
