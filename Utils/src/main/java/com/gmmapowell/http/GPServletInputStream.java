package com.gmmapowell.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.servlet.ServletInputStream;

import com.gmmapowell.exceptions.UtilException;

public class GPServletInputStream extends ServletInputStream {
	private final ConnectionHandler conn;
	private ByteBuffer buffer;
	int cnt = 0;
	private final int maxchars;
	private int pushback = -1;

	public GPServletInputStream(ConnectionHandler conn, ByteBuffer buffer, int maxchars) {
		this.conn = conn;
		this.buffer = buffer;
		this.maxchars = maxchars;
	}

	@Override
	public int read() throws IOException {
//		InlineServer.logger.info("Read() called");
		if (pushback != -1)
		{
			int b = pushback;
			pushback = -1;
			return b;
		}
		if (maxchars >= 0 && cnt >= maxchars)
			return -1;
		while (!buffer.hasRemaining()) {
			buffer.clear();
			conn.wantMore(true);
		}
		int b = buffer.get();
		cnt ++; 
//		InlineServer.logger.info("read = " + (char)b + " cnt = " + cnt);
		return b;
	}

	public void flush() throws IOException {
		if (maxchars == -1)
			return;
		while (cnt < maxchars) {
			read();
		}
	}

	public void pushback(int b) {
		if (b == -1)
			throw new UtilException("Cannot push -1");
		pushback = b;
	}
}
