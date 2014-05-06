package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public class ExceptionHandler {
	private final int exType;
	private final Marker from;
	private final Marker to;
	private final Marker handler;

	public ExceptionHandler(int exType, Marker from, Marker to, Marker handler) {
		this.exType = exType;
		this.from = from;
		this.to = to;
		this.handler = handler;
	}

	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(from.getLocation());
		dos.writeShort(to.getLocation());
		dos.writeShort(handler.getLocation());
		dos.writeShort(exType);
	}

}
