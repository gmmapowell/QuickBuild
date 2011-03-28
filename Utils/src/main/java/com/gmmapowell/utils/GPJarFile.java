package com.gmmapowell.utils;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
			return new GPJarEntry(en.nextElement());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private JarFile jf;
	
	public GPJarFile(File f) {
		try {
			jf = new JarFile(FileUtils.relativePath(f));
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	@Override
	public Iterator<GPJarEntry> iterator() {
		return new JEIterator();
	}

}
