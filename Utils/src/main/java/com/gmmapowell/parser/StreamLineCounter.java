package com.gmmapowell.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;

import com.gmmapowell.exceptions.UtilException;

public class StreamLineCounter implements LineCounter {
	private LineNumberReader lnr;

	public StreamLineCounter(InputStream stream) {
		lnr = new LineNumberReader(new InputStreamReader(stream));
	}

	public StreamLineCounter(Reader r) {
		lnr = new LineNumberReader(r);
	}

	@Override
	public int getLineNumber() {
		return lnr.getLineNumber();
	}

	@Override
	public String readLine() {
		try {
			return lnr.readLine();
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	@Override
	public void close() {
		try {
			lnr.close();
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}
}
