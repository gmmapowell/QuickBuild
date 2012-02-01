package com.gmmapowell.utils;

import java.io.IOException;
import java.io.InputStream;

public class LoggingInputStream extends InputStream {
	private int pos = 0;
	private final InputStream in;
	
	public LoggingInputStream(InputStream in) {
		this.in = in;
	}

	@Override
	public int read() throws IOException {
		if (pos % 20 == 0)
		{
			System.out.println();
			System.out.print(StringUtil.digits(pos, 3) + ":");
		}
		int ret = in.read();
		System.out.print((char)ret);
		pos++;
		return ret;
	}
}