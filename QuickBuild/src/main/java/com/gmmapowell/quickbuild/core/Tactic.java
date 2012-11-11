package com.gmmapowell.quickbuild.core;

import java.util.Set;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;

public interface Tactic {

	public Strategem belongsTo();

	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug);

	public String identifier();
	
	public void addProcessDependency(Tactic earlier);

	Set<Tactic> getProcessDependencies();
}
