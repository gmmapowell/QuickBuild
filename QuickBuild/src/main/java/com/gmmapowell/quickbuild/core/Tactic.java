package com.gmmapowell.quickbuild.core;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;

public interface Tactic {

	public Strategem belongsTo();

	public BuildStatus execute(BuildContext cxt);
}
