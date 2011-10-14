package com.gmmapowell.utils;

import com.gmmapowell.exceptions.UtilException;

public class WriteOnce<T> {
	private boolean isSet;
	private T value;
	
	public void set(T setTo) {
		if (isSet)
			throw new UtilException("Cannot set a writeOnce variable more than once");
		value = setTo;
		isSet = true;
	}

	// This can be used to confirm "null" if you reach a point where it hasn't been set.
	public void complete(T value) {
		if (!isSet)
			set(value);
	}

	public T get() {
		if (!isSet)
			throw new UtilException("Cannot read a writeOnce variable before setting it");
		return value;
	}

	public void nullIfUnwritten() {
		if (!isSet)
		{
			value = null;
			isSet = true;
		}
	}


}
