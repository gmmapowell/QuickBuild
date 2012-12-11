package com.gmmapowell.xml;

public class Location {
	private final String file;
	public final int line;
	public final int column;

	public Location(String file, int line, int column) {
		this.file = file;
		this.line = line;
		this.column = column;
	}
	
	@Override
	public String toString() {
		return line +":" + column;
	}

	public String getFile() {
		return file;
	}
}
