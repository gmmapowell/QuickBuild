package com.gmmapowell.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.gmmapowell.exceptions.GPJarException;
import com.gmmapowell.exceptions.UtilException;

public class GPJarFile implements Iterable<GPJarEntry> {
	class JEIterator implements Iterator<GPJarEntry> {
		private Enumeration<JarEntry> en = jf.entries(); 
		
		@Override
		public boolean hasNext() {
			return en.hasMoreElements();
		}

		@Override
		public GPJarEntry next() {
			return new GPJarEntry(GPJarFile.this, en.nextElement());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private JarFile jf;
	private boolean stillOpen;
	
	public GPJarFile(File f) {
		try {
			jf = new JarFile(FileUtils.relativePath(f));
			stillOpen = true;
		} catch (IOException e) {
			throw new GPJarException(e);
		}
	}

	@Override
	public Iterator<GPJarEntry> iterator() {
		return new JEIterator();
	}

	public InputStream getInputStream(JarEntry entry) {
		try
		{
			return jf.getInputStream(entry);
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	public void close()
	{
		stillOpen = false;
		try {
			if (jf != null)
				jf.close();
		} catch (IOException e) {
			throw new GPJarException(e);
		}
	}

	public boolean isOpen() {
		return stillOpen;
	}

	public GPJarEntry get(String name) {
		ZipEntry ret = jf.getEntry(name);
		System.out.println("Looking for " + name + " in " + this);
		if (ret == null || !(ret instanceof JarEntry))
			return null;
		return new GPJarEntry(this, (JarEntry) ret);
	}

	@Override
	public String toString() {
		return "[JAR: " + jf.getName()+"]";
	}
}
