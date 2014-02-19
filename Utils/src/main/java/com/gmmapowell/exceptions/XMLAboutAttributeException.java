package com.gmmapowell.exceptions;

import com.gmmapowell.xml.Location;

@SuppressWarnings("serial")
public class XMLAboutAttributeException extends XMLUtilException implements XMLProcessingException {
	private final String attr;

	public XMLAboutAttributeException(Location start, Location end, String attr, String message) {
		super(start, end, message);
		this.attr = attr;
	}

	public String getAttribute() {
		return attr;
	}

}
