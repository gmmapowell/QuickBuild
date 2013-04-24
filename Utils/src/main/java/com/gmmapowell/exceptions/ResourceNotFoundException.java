package com.gmmapowell.exceptions;

@SuppressWarnings("serial")
public class ResourceNotFoundException extends UtilException implements UtilPredictableException {

	public ResourceNotFoundException(String msg) {
		super(msg);
	}

}
