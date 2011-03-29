package com.gmmapowell.exceptions;

import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("serial")
public class UtilException extends RuntimeException {

	public UtilException(String string) {
		super(string);
	}

	public UtilException(String string, Throwable ex) {
		super(string, ex);
	}

	public static RuntimeException wrap(Throwable ex) {
		if (ex instanceof RuntimeException)
			return (RuntimeException)ex;
		else if (ex instanceof InvocationTargetException)
			return wrap(ex.getCause());
		else
			return new UtilException("A checked exception was caught", ex);
	}

}
