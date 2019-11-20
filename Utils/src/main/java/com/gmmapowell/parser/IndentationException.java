package com.gmmapowell.parser;

import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class IndentationException extends UtilException {

	public IndentationException() {
		super("Invalid indentation");
	}

}
