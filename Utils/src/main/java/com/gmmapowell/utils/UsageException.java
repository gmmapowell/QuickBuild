package com.gmmapowell.utils;

import java.util.List;

import org.zinutils.utils.StringUtil;

@SuppressWarnings("serial")
public class UsageException extends RuntimeException {

	public UsageException(String usage) {
		super(usage);
	}
	
	public UsageException(List<String> errors) {
		super(StringUtil.concatVertically(errors));
	}
	
}
