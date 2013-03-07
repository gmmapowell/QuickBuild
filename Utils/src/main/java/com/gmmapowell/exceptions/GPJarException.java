package com.gmmapowell.exceptions;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("serial")
public class GPJarException extends RuntimeException {

	public GPJarException(IOException e) {
		super(e);
	}

	public GPJarException(File f, IOException e) {
		super("Error with Jar file " + f, e);
	}

}
