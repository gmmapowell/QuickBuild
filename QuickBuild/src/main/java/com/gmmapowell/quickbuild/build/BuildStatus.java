package com.gmmapowell.quickbuild.build;

public enum BuildStatus {
	SUCCESS, IGNORED, SKIPPED,
	RETRY, TEST_FAILURES, BROKEN;
	
	public boolean isGood() { 
		return this == SUCCESS || this == IGNORED || this == SKIPPED;
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
