package com.gmmapowell.quickbuild.build;

import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.Project;

public interface BuildCommand {

	public Project getProject();

	// TODO: this feels like "special-case" duplication.  Packages should just be resources like anything else.
	// We should have a "PackageResource" class
	public Set<String> getPackagesProvided();

	public List<BuildResource> generatedResources();

	public BuildStatus execute(BuildContext cxt);
}
