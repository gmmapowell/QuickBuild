package com.gmmapowell.quickbuild.build;

public enum BuildStatus {
	SUCCESS, IGNORED,
	RETRY, TEST_FAILURES, BROKEN;
	
	public boolean isGood() { 
		return this == SUCCESS || this == IGNORED;
	}
	
	public boolean tryAgain() {
		return this == RETRY;
	}
	
	public boolean moveOn() {
		return isGood() || this == TEST_FAILURES;
	}
	
	public boolean isBroken() {
		return this == BROKEN;
	}
}
