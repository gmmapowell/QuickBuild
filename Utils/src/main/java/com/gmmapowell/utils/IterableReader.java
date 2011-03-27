package com.gmmapowell.utils;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Iterator;

import com.gmmapowell.exceptions.UtilException;

public class IterableReader implements Iterable<String> {
	private class LineIterator implements Iterator<String> {
		private String next;

		@Override
		public boolean hasNext() {
			if (next != null)
				return true;
			try
			{
				next = lnr.readLine();
				if (next == null)
					return false;
				return true;
			}
			catch (IOException ex)
			{
				return false;
			}
		}

		@Override
		public String next() {
			if (!hasNext())
				throw new UtilException("There is no next element");
			String ret = next;
			next = null;
			return ret;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private LineNumberReader lnr;

	public IterableReader(Reader reader) {
		if (reader instanceof LineNumberReader)
			this.lnr = (LineNumberReader)reader;
		else
			this.lnr = new LineNumberReader(reader);
	}

	@Override
	public Iterator<String> iterator() {
		return new LineIterator();
	}
}
