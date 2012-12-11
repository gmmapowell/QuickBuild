package com.gmmapowell.exceptions;

@SuppressWarnings("serial")
public class XMLMissingAttributeException extends XMLUtilException implements XMLProcessingException {

	public XMLMissingAttributeException(String message) {
		super(message);
	}

}
