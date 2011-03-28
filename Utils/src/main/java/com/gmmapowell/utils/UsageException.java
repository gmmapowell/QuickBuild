package com.gmmapowell.utils;

import java.util.List;

@SuppressWarnings("serial")
public class UsageException extends RuntimeException {

	public UsageException(List<String> errors) {
		super(StringUtil.concatVertically(errors));
	}
	
}
