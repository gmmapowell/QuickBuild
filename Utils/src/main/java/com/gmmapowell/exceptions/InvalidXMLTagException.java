package com.gmmapowell.exceptions;

import com.gmmapowell.xml.XMLElement;

@SuppressWarnings("serial")
public class InvalidXMLTagException extends XMLUtilException implements XMLProcessingException {
	public final XMLElement xe;
	public final String which;
	public final Object callbacks;

	public InvalidXMLTagException(XMLElement xe, String which, Object callbacks) {
		super(xe.getStartLocation(), xe.getEndLocation(), "The object " + callbacks + " does not have a handler for tag " + which + " in element " + xe);
		this.xe = xe;
		this.which = which;
		this.callbacks = callbacks;
	}

	public String getAttribute() {
		return null;
	}
}
