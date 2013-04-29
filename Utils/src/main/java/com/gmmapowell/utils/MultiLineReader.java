package com.gmmapowell.utils;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class MultiLineReader extends Reader {
	private StringBuilder sb = new StringBuilder();
	private int pointer = 0;
	
	public MultiLineReader(List<String> lines) {
		for (String s : lines) {
			sb.append(s);
			sb.append("\n");
		}
	}

	@Override
	public int read(char[] arg0, int arg1, int arg2) throws IOException {
		if (pointer >= sb.length())
			return -1;
		if (pointer+arg2 >= sb.length())
			arg2 = sb.length()-pointer;
		for (int i=0;i<arg2;i++) {
			arg0[arg1++] = sb.charAt(pointer++); 
		}
		return arg2;
	}

	@Override
	public void close() throws IOException {
		// Nothing to do
	}
}
