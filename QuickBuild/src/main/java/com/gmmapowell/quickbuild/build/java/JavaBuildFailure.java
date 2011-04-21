package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

@SuppressWarnings("serial")
public class JavaBuildFailure extends QuickBuildException {

	public JavaBuildFailure(String string) {
		super(string);
	}

}
