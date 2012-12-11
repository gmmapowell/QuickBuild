package com.gmmapowell.exceptions;

@SuppressWarnings("serial")
public class XMLUnprocessedAttributeException extends XMLUtilException implements XMLProcessingException {

	private final String attr;

	public XMLUnprocessedAttributeException(String attr, String message) {
		super(message);
		this.attr = attr;
	}

	public String getAttribute() {
		return attr;
	}

}
