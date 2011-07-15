package com.gmmapowell.http;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

public class GPServletOutputStream extends ServletOutputStream {
	private final OutputStream os;

	public GPServletOutputStream(OutputStream os) {
		this.os = os;
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		os.write(b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}
}