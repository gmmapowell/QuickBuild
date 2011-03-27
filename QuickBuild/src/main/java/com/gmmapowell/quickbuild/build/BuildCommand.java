package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.config.Project;

public interface BuildCommand {

	public boolean execute(BuildContext cxt);

	public Project getProject();

}
