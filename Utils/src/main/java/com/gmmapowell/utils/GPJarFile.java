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
	public class JEIterable implements Iterable<GPJarEntry> {
		private final String prefix;

		public JEIterable(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public Iterator<GPJarEntry> iterator() {
			return new JEIterator(prefix);
		}

	}

	class JEIterator implements Iterator<GPJarEntry> {
		private Enumeration<JarEntry> en = jf.entries();
		private final String prefix;
		private JarEntry cuedUp; 
		
		public JEIterator(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public boolean hasNext() {
			if (cuedUp != null)
				return true;
			while (en.hasMoreElements()) {
				cuedUp = en.nextElement();
				if (prefix == null || cuedUp.getName().startsWith(prefix))
					return true;
				cuedUp = null;
			}
			return false;
		}

		@Override
		public GPJarEntry next() {
			if (!hasNext())
				throw new IndexOutOfBoundsException();
			GPJarEntry ret = new GPJarEntry(GPJarFile.this, cuedUp);
			cuedUp = null;
			return ret;
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
			throw new GPJarException(f, e);
		}
	}

	public String getFileName() {
		return jf.getName();
	}

	@Override
	public Iterator<GPJarEntry> iterator() {
		return new JEIterator(null);
	}

	public Iterable<GPJarEntry> startsWith(String prefix) {
		return new JEIterable(prefix);
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
		if (ret == null || !(ret instanceof JarEntry))
			return null;
		return new GPJarEntry(this, (JarEntry) ret);
	}

	/** Extract contents of JAR/ZIP file to directory
	 * 
	 * @param dir the directory to extract to
	 * @return this object
	 */
	public GPJarFile extractAll(File dir) {
		for (GPJarEntry je : this) {
			if (je.isDirectory())
				FileUtils.assertDirectory(je.getFile());
			else {
				File f = FileUtils.combine(dir, je.getFile());
				FileUtils.assertDirectory(f.getParentFile());
				FileUtils.copyStreamToFile(je.asStream(), f);
			}
		}
		return this;
	}

	@Override
	public String toString() {
		return "[JAR: " + jf.getName()+"]";
	}
}
