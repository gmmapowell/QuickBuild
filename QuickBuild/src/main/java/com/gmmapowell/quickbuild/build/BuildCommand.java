package com.gmmapowell.quickbuild.build;

import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.Project;

public interface BuildCommand {

	public Project getProject();

	public Set<String> getPackagesProvided();

	public List<BuildResource> generatedResources();

	public BuildStatus execute(BuildContext cxt);
}
