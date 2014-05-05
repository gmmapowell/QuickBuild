package com.gmmapowell.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.servlet.ServletOutputStream;

public class GPServletOutputStream extends ServletOutputStream {
	private final SocketChannel chan;
	private final int myunique;
	private static int unique = 0;

	public GPServletOutputStream(SocketChannel chan) {
		this.chan = chan;
		this.myunique = ++unique;
		InlineServer.logger.trace(Thread.currentThread().getName() + ": creating " + this);
	}

	@Override
	public void write(int b) throws IOException {
//		InlineServer.logger.finest("Writing " + new String(new byte[] { (byte)b }));
		ByteBuffer src = ByteBuffer.allocate(1);
		src.put((byte)b);
		src.rewind();
		chan.write(src);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
//		InlineServer.logger.finest("Writing " + new String(b));
		ByteBuffer src = ByteBuffer.allocate(b.length);
		src.put(b, 0, b.length);
		src.rewind();
		chan.write(src);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
//		InlineServer.logger.finest("Writing " + new String(b));
		if (len <= 0)
			return;
		ByteBuffer src = ByteBuffer.allocate(len);
		src.put(b, off, len);
		src.rewind();
		chan.write(src);
	}
	
	@Override
	public void close() throws IOException {
		InlineServer.logger.trace(Thread.currentThread() + " " + this + " closing stream");
		super.close();
		chan.close();
	}
	
	@Override
	public String toString() {
		return "GPSos" + myunique;
	}
}
