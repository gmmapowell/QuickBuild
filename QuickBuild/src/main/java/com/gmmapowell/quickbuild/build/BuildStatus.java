package com.gmmapowell.quickbuild.build;

public enum BuildStatus {
	// It came out OK (enough)
	SUCCESS, // we did work and it went fine
	CLEAN,	 // it was already clean, so move on 
	SKIPPED, // we skipped this step because there didn't seem anything to do
	DEFERRED, // we didn't do it for some (unspecified) reason, but don't worry about it.  Get back to me!
	
	// It didn't work out so well ...
	RETRY,			// But I made changes, so try again!
	TEST_FAILURES,	// Tests failed, but don't break the build
	BROKEN;			// I just can't go on!
	
	public boolean isGood() { 
		return this == SUCCESS || this == CLEAN || this == SKIPPED || this == DEFERRED;
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
	
	public boolean isExit1() {
		return needsRebuild();
	}

	public boolean builtResources() {
		return moveOn();
	}

	// This is before we build ... do we need to build?
	public boolean needsBuild() {
		return this == SUCCESS || this == RETRY || this == DEFERRED;
	}
}
