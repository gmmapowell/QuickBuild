package com.gmmapowell.exceptions;

import com.gmmapowell.xml.Location;

@SuppressWarnings("serial")
public class XMLMissingAttributeException extends XMLUtilException implements XMLProcessingException {

	public XMLMissingAttributeException(Location start, Location end, String message) {
		super(start, end, message);
	}

	public String getAttribute() {
		return null;
	}

}
