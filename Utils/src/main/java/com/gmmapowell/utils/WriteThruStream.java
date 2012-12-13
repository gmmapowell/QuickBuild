package com.gmmapowell.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WriteThruStream {
	private InputEnd inputEnd = new InputEnd();
	private OutputEnd outputEnd = new OutputEnd();
	private int writePtr = 0;
	private int readPtr = 0;
	private byte[] bytes = new byte[10000];
	private boolean closed;
	private boolean cancelled;

	public class OutputEnd extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			synchronized(WriteThruStream.this) {
				if (cancelled)
					throw new IOException("InputEnd closed");
				while (space() < 5)
				{
					try {
						WriteThruStream.this.wait();
					} catch (InterruptedException e) {
					}
					if (cancelled)
						throw new IOException("InputEnd closed");
				}
				bytes[writePtr++] = (byte)b;
//				System.out.print(">"+StringUtil.hex(b, 2));
				WriteThruStream.this.notifyAll();
				if (writePtr >= bytes.length)
					writePtr = 0;
			}
		}

		@Override
		public void close() throws IOException {
			synchronized(WriteThruStream.this) {
				super.close();
				closed = true;
				WriteThruStream.this.notifyAll();
			}
		}

		public void cancel() {
			WriteThruStream.this.cancel();
		}
		
		/* I would like to do this, but it feels hard to get all the numbers right...
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			// TODO Auto-generated method stub
			super.write(b, off, len);
		}
	*/
	}
	
	public class InputEnd extends InputStream {

		@Override
		public int read() throws IOException {
			synchronized(WriteThruStream.this) {
				if (cancelled)
					throw new IOException("OutputEnd closed");
				while (size() == 0)
				{
					if (cancelled)
						throw new IOException("OutputEnd closed");
					if (closed)
					{
//						System.out.println();
						return -1;
					}
					try {
						WriteThruStream.this.wait();
					} catch (InterruptedException e) {
					}
				}
				int ret = bytes[readPtr++];
//				System.out.print("<"+StringUtil.hex(ret, 2));
				WriteThruStream.this.notifyAll();
				if (readPtr >= bytes.length)
				{
//					System.out.print(".");
					readPtr = 0;
				}
				return ret&0xff;
			}
		}

		@Override
		public void close() throws IOException {
			synchronized(WriteThruStream.this) {
				super.close();
				cancel();
				WriteThruStream.this.notifyAll();
			}
		}
		
		public void cancel() {
			WriteThruStream.this.cancel();
		}
		/* I would like to do this, but it feels hard to get all the numbers right...
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			// TODO Auto-generated method stub
			return super.read(b, off, len);
		}
		*/
	}

	public InputStream getInputEnd() {
		return inputEnd;
	}

	public OutputStream getOutputEnd() {
		return outputEnd;
	}
	
	private int space() {
		return bytes.length - size();
	}

	private int size() {
		if (readPtr == writePtr)
			return 0;
		else if (readPtr < writePtr)
			return writePtr-readPtr;
		else
			return bytes.length+writePtr-readPtr;
	}

	public void cancel() {
		synchronized (this)
		{
			cancelled = true;
			this.notifyAll();
		}
	}
}
