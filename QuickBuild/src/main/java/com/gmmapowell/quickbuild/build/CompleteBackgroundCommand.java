package com.gmmapowell.quickbuild.build;

import org.zinutils.system.RunProcess;

public interface CompleteBackgroundCommand {

	BuildStatus completeCommand(BuildContext cxt, RunProcess proc);

	String getLabel();
}
