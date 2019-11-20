package com.gmmapowell.parser;

import org.zinutils.exceptions.UtilException;

public class PushLineCounter implements LineCounter {
	private int line;
	private final StringBuilder input = new StringBuilder();
	private boolean closed = false;

	public PushLineCounter() {
		line = 0;
	}

	public void pushLine(String s) {
		input.append(s);
		input.append("\n");
	}

	public void push(String s) {
		input.append(s);
	}

	@Override
	public int getLineNumber() {
		return line;
	}

	@Override
	public String readLine() {
		if (closed)
			return null;
		
		int idx = input.indexOf("\n");
		if (idx == -1)
			throw new UtilException("Out of sync use of PushLineCounter");
		
		String ret = input.substring(0, idx);
		input.delete(0, idx+1);
		line++;
		return ret;
	}

	@Override
	public void close() {
		closed = true;
	}
}
