package com.gmmapowell.quickbuild.build;

public enum BuildStatus {
	// It came out OK (enough)
	SUCCESS, // we did work and it went fine
	CLEAN,	 // it was already clean, so move on 
	SKIPPED, // we skipped this step because there didn't seem anything to do
	NOTAPPLICABLE, // a "buildif" command said not to 
	NOTCRITICAL,   // not on the critical path
	// It didn't work out so well ...
	RETRY,			// But I made changes, so try again!
	TEST_FAILURES,	// Tests failed, but don't break the build
	BROKEN_DEPENDENCIES, // There were broken dependencies
	BROKEN;			// I just can't go on!
	
	public boolean isGood() { 
		return this == SUCCESS || this == CLEAN || this == SKIPPED || this == NOTAPPLICABLE || this == NOTCRITICAL;
	}
	
	public boolean tryAgain() {
		return this == RETRY;
	}
	
	public boolean moveOn() {
		return isGood() || this == TEST_FAILURES || this == BROKEN_DEPENDENCIES;
	}
	
	public boolean isBroken() {
		return this == BROKEN;
	}

	public boolean needsRebuild() {
		return isBroken() || this == TEST_FAILURES || this == BROKEN_DEPENDENCIES;
	}
	
	public boolean isExit1() {
		return needsRebuild();
	}
	
	public boolean isWorthReporting() {
		return this == BROKEN || this == TEST_FAILURES;
	}

	public boolean builtResources() {
		return moveOn();
	}

	// This is before we build ... do we need to build?
	public boolean needsBuild() {
		return this == SUCCESS || this == RETRY;
	}

	public boolean partialFail() {
		return this == TEST_FAILURES;
	}
}
