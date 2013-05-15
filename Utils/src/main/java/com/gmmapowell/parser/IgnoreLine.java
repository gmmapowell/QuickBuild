package com.gmmapowell.parser;

import com.gmmapowell.exceptions.UtilException;

public class IgnoreLine implements Parent<Object> {

	private IgnoreLine() { }
	
	public static final IgnoreLine INSTANCE = new IgnoreLine();

	@Override
	public void addChild(Object obj) {
		throw new UtilException("Cannot add a child to an IgnoreLine");
	}
}
