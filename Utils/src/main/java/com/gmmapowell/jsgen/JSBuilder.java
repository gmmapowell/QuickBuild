package com.gmmapowell.jsgen;

public class JSBuilder {
	private final StringBuilder sb = new StringBuilder();

	public void open() {
		append("{");
	}
	
	public void close() {
		append("}");
	}

	public void startList() {
		append("[");
	}
	
	public void endList() {
		append("]");
	}
	
	public void append(String s) {
		sb.append(s);
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}
