package com.gmmapowell.exceptions;

@SuppressWarnings("serial")
public class XMLUnprocessedAttributeException extends XMLAboutAttributeException implements XMLProcessingException {

	public XMLUnprocessedAttributeException(String attr, String message) {
		super(attr, message);
	}

}
