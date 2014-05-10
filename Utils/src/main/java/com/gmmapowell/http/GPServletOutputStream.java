package com.gmmapowell.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.servlet.ServletOutputStream;

import com.gmmapowell.sync.SyncUtils;

public class GPServletOutputStream extends ServletOutputStream {
	private final SocketChannel chan;
	private final int myunique;
	private static int unique = 0;
//	private int total = 0;

	public GPServletOutputStream(SocketChannel chan) {
		this.chan = chan;
		this.myunique = ++unique;
		InlineServer.logger.trace(Thread.currentThread().getName() + ": creating " + this);
	}

	@Override
	public void write(int b) throws IOException {
//		InlineServer.logger.info("Writing (" + b + ") ");
		byte[] arr = new byte[1];
		arr[0] = (byte)b;
		write(arr, 0, 1);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}
	
	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
//		InlineServer.logger.info("Writing (" + off + ","+len+") " + StringUtil.hex(b, off, len) + " :: " + new String(b, off, len));
		if (len <= 0)
			return;
		ByteBuffer src = ByteBuffer.allocate(len);
		src.put(b, off, len);
		src.rewind();
		while (len > 0) {
			int cnt = chan.write(src);
			if (cnt == 0) {
				SyncUtils.sleep(10);
				continue;
			}
			off += cnt;
			len -= cnt;
//			total += cnt;
//			InlineServer.logger.info("Wrote " + cnt + " bytes for a total of " + total + " chan = " + chan.isBlocking());
		}
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
