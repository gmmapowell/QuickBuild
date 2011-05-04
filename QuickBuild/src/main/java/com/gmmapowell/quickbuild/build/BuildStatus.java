package com.gmmapowell.quickbuild.build;

public enum BuildStatus {
	SUCCESS, IGNORED, SKIPPED, DEFERRED,
	RETRY, TEST_FAILURES, BROKEN;
	
	public boolean isGood() { 
		return this == SUCCESS || this == IGNORED || this == SKIPPED || this == DEFERRED;
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

	public boolean needsRebuild() {
		return isBroken() || this == TEST_FAILURES;
	}
}
