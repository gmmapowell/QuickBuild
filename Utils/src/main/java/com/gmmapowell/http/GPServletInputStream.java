package com.gmmapowell.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.servlet.ServletInputStream;

import com.gmmapowell.exceptions.UtilException;

public class GPServletInputStream extends ServletInputStream {

	private final SocketChannel chan;
	int cnt = 0;
	private final int maxchars;
	private int pushback = -1;

	public GPServletInputStream(SocketChannel chan, int maxchars) {
		this.chan = chan;
		this.maxchars = maxchars;
	}

	@Override
	public int read() throws IOException {
		if (pushback != -1)
		{
			int b = pushback;
			pushback = -1;
			return b;
		}
		if (maxchars >= 0 && cnt >= maxchars)
			return -1;
		ByteBuffer dst = ByteBuffer.allocate(1);
		chan.read(dst);
		dst.rewind();
		int b = dst.get();
		cnt ++; 
//		InlineServer.logger.info("read = " + (char)b + " cnt = " + cnt);
		return b;
	}

	public void flush() throws IOException {
		if (maxchars == -1)
			return;
		ByteBuffer dst = ByteBuffer.allocate(maxchars-cnt);
		while (dst.hasRemaining())
			cnt += chan.read(dst);
	}

	public void pushback(int b) {
		if (b == -1)
			throw new UtilException("Cannot push -1");
		pushback = b;
	}
}
