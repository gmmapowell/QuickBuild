package com.gmmapowell.exceptions;

import org.xml.sax.SAXParseException;

@SuppressWarnings("serial")
public class XMLParseException extends RuntimeException {

	public XMLParseException(SAXParseException ex) {
		super(ex);
	}

}
