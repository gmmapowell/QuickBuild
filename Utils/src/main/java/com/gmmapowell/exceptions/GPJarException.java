package com.gmmapowell.exceptions;

import java.io.IOException;

@SuppressWarnings("serial")
public class GPJarException extends RuntimeException {

	public GPJarException(IOException e) {
		super(e);
	}

}
