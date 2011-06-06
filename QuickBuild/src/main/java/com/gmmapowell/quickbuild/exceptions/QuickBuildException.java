package com.gmmapowell.quickbuild.exceptions;

@SuppressWarnings("serial")
public class QuickBuildException extends RuntimeException {

	public QuickBuildException(String string) {
		super(string);
	}

	public QuickBuildException(String string, Exception ex) {
		super(string, ex);
	}

}
