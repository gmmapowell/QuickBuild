package com.gmmapowell.exceptions;

import java.lang.reflect.Constructor;
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

	public static Exception reconstitute(String exClass, String msg) {
		try
		{
			Class<?> forName = Class.forName(exClass);
			try
			{
				Constructor<?> ctor = forName.getConstructor(String.class);
				if (ctor != null)
					return (Exception) ctor.newInstance(msg);
			}
			catch (NoSuchMethodException e2)
			{
			}
			return (Exception) forName.newInstance();
		}
		catch (Exception ex)
		{
			return new UtilException("Exception of unrecognized class " + exClass + " thrown by server", ex);
		}
	}
}
