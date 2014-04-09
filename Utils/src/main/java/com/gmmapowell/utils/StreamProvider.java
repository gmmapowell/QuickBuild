package com.gmmapowell.utils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.gmmapowell.exceptions.NotImplementedException;
import com.gmmapowell.exceptions.UtilException;

public interface StreamProvider {
	public InputStream read();
	public OutputStream write();
	public byte[] readAll();
	public void write(byte[] bs);

	public class File implements StreamProvider {
		private final java.io.File file;

		public File(String path) {
			this.file = new java.io.File(path);
		}

		public File(java.io.File f) {
			this.file = f;
		}

		@Override
		public InputStream read() {
			try {
				return new FileInputStream(file);
			} catch (IOException ex) {
				throw UtilException.wrap(ex);
			}
		}

		@Override
		public OutputStream write() {
			try {
				return new FileOutputStream(file);
			} catch (IOException ex) {
				throw UtilException.wrap(ex);
			}
		}

		@Override
		public byte[] readAll() {
			return FileUtils.readFileAsBytes(file);
		}

		@Override
		public void write(byte[] bs) {
			FileUtils.copyStreamToFile(new ByteArrayInputStream(bs), file);
		}
	}

	public class FromJar implements StreamProvider {
		private final java.io.File jarf;
		private final String entry;

		public FromJar(java.io.File jarf, String entry) {
			this.jarf = jarf;
			this.entry = entry;
		}

		@Override
		public InputStream read() {
			GPJarFile jf = new GPJarFile(jarf);
			return jf.get(entry).asStream(); // I believe this will leak a JF resource ... I think we need to wrap this to propagate-close the parent Jar
		}

		@Override
		public OutputStream write() {
			throw new NotImplementedException();
		}

		@Override
		public byte[] readAll() {
			GPJarFile jf = new GPJarFile(jarf);
			byte[] ret = jf.get(entry).getBytes();
			jf.close();
			return ret;
		}

		@Override
		public void write(byte[] bs) {
			throw new NotImplementedException();
		}

	}

}
