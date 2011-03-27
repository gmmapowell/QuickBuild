package com.gmmapowell.quickbuild.build;

import java.util.Set;

import com.gmmapowell.quickbuild.config.Project;

public interface BuildCommand {

	public boolean execute(BuildContext cxt);

	public Project getProject();

	public Set<String> getPackagesProvided();
}
