package com.gmmapowell.parser;

import com.gmmapowell.exceptions.UtilException;

public class IgnoreLine implements Parent<IgnoreLine> {

	private IgnoreLine() { }
	
	public static final IgnoreLine INSTANCE = new IgnoreLine();

	@Override
	public void addChild(IgnoreLine obj) {
		throw new UtilException("Cannot add a child to an IgnoreLine");
	}
}
