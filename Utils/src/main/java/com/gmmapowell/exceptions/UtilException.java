package com.gmmapowell.exceptions;

@SuppressWarnings("serial")
public class UtilException extends RuntimeException {

	public UtilException(String string) {
		super(string);
	}

	public UtilException(String string, Exception ex) {
		super(string, ex);
	}

	public static RuntimeException wrap(Exception ex) {
		if (ex instanceof RuntimeException)
			return (RuntimeException)ex;
		return new UtilException("A checked exception was caught", ex);
	}

}
