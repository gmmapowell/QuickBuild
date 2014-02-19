package com.gmmapowell.exceptions;

import com.gmmapowell.xml.Location;

@SuppressWarnings("serial")
public class XMLUtilException extends UtilException {
	public XMLUtilException(Location start, Location end, String message) {
		super(message);
	}
}
