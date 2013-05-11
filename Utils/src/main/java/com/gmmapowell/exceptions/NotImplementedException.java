package com.gmmapowell.exceptions;

@SuppressWarnings("serial")
public class NotImplementedException extends UtilException {
	public NotImplementedException() {
		super("Not Implemented");
	}

	public NotImplementedException(String expl) {
		super("Not Implemented: " + expl);
	}
}