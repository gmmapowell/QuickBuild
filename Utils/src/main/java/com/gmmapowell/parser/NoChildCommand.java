package com.gmmapowell.parser;

import org.zinutils.exceptions.UtilException;

public class NoChildCommand implements Parent<Object> {
	@Override
	public void addChild(Object obj) {
		throw new UtilException("Cannot add '" + obj + "' to '" + this + "' because it does not allow children");
	}

}
