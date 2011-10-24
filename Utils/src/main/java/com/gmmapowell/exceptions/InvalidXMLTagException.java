package com.gmmapowell.exceptions;

import com.gmmapowell.xml.XMLElement;

@SuppressWarnings("serial")
public class InvalidXMLTagException extends XMLUtilException {

	private final XMLElement xe;
	private final String which;
	private final Object callbacks;

	public InvalidXMLTagException(XMLElement xe, String which, Object callbacks) {
		super("The object " + callbacks + " does not have a handler for tag " + which + " in element " + xe);
		this.xe = xe;
		this.which = which;
		this.callbacks = callbacks;
	}

}
