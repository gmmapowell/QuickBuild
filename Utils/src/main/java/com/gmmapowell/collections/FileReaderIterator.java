package com.gmmapowell.collections;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.gmmapowell.exceptions.UtilException;

public class FileReaderIterator implements Iterator<String> {
	private LineNumberReader reader;
	private String next;

	public FileReaderIterator(File file) throws FileNotFoundException {
		reader = new LineNumberReader(new FileReader(file));
	}

	public FileReaderIterator(InputStream resourceAsStream) {
		reader = new LineNumberReader(new InputStreamReader(resourceAsStream));
	}

	@Override
	public boolean hasNext() {
		if (next == null) {
			if (reader == null)
				return false;
			try {
				next = reader.readLine();
			} catch (IOException e) {
			}
			if (next == null)
				reader = null;
		}
		return next != null;
	}

	@Override
	public String next() {
		if (!hasNext())
			throw new NoSuchElementException();
		String ret = next;
		next = null;
		return ret;
	}

	@Override
	public void remove() {
		throw new UtilException("Remove not supported");
	}

	public static Iterable<String> overFile(File file) {
		try {
			final FileReaderIterator it = new FileReaderIterator(file);
			return new Iterable<String>() {
				@Override
				public Iterator<String> iterator() {
					return it;
				}
			};
		} catch (FileNotFoundException e) {
			throw UtilException.wrap(e);
		}
	}

	public static Iterable<String> overStream(InputStream stream) {
		final FileReaderIterator it = new FileReaderIterator(stream);
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return it;
			}
		};
	}

}
