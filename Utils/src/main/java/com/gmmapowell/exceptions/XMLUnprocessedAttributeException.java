package com.gmmapowell.exceptions;

import com.gmmapowell.xml.Location;

@SuppressWarnings("serial")
public class XMLUnprocessedAttributeException extends XMLAboutAttributeException implements XMLProcessingException {

	public XMLUnprocessedAttributeException(Location start, Location end, String attr, String message) {
		super(start, end, attr, message);
	}

}
