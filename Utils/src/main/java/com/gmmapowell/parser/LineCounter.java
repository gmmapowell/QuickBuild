package com.gmmapowell.parser;

public interface LineCounter {
	int getLineNumber();
	String readLine();
	void close();
}
