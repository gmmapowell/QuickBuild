package com.gmmapowell.http;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

public class GPServletInputStream extends ServletInputStream {

	private final InputStream reader;
	int cnt = 0;
	private final int maxchars;

	public GPServletInputStream(InputStream reader, int maxchars) {
		this.reader = reader;
		this.maxchars = maxchars;
	}

	@Override
	public int read() throws IOException {
		if (cnt >= maxchars)
			return -1;
		int b = reader.read();
		cnt ++; 
//		InlineServer.logger.info("read = " + (char)b + " cnt = " + cnt);
		return b;
	}

}
