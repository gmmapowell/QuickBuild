package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildResource;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.DirectoryResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

// TODO: add the checking logic
// TODO: add the FileUtils functions
// TODO: resources
public class CopyDirectoryCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, BuildCommand {
	private Project project;
	private String fromProjectName;
	private File fromProjectDir;
	private String fromDirectory;
	private String toProjectName;
	private File toProjectDir;
	private String toDirectory;
	private DirectoryResource fromResource;
	private DirectoryResource toResource;

	@SuppressWarnings("unchecked")
	public CopyDirectoryCommand(TokenizedLine toks) {
		// TODO: want 4 args
		toks.process(this,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "fromProjectName", "from project"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "fromDirectory", "directory"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "toProjectName", "to project"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "toDirectory", "directory")
		);
		fromProjectDir = FileUtils.findDirectoryNamed(fromProjectName);
		toProjectDir = FileUtils.findDirectoryNamed(toProjectName);
	}

	@Override
	public void applyConfig(Config config) {
		project = new Project("copy", fromProjectName, fromProjectDir, config.getOutput());
		fromResource = new DirectoryResource(project, new File(fromProjectDir, fromDirectory));
		toResource = new DirectoryResource(project, new File(toProjectDir, toDirectory));
	}

	@Override
	public Project project() {
		return project;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<? extends BuildCommand> buildCommands() {
		return CollectionUtils.listOf((BuildCommand)this);
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public Set<String> getPackagesProvided() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BuildResource> generatedResources() {
		return CollectionUtils.listOf((BuildResource)fromResource, toResource);
	}

	@Override
	public BuildStatus execute(BuildContext cxt) {
		if (!cxt.requiresBuiltResource(this, fromResource))
			return BuildStatus.RETRY;
		FileUtils.assertDirectory(toResource.getDirectory());
		FileUtils.copyRecursive(fromResource.getDirectory(), toResource.getDirectory());
		cxt.addBuiltResource(toResource);
		return BuildStatus.SUCCESS;
	}

	@Override
	public String toString() {
		return "Copy " + fromResource + " to " + toResource;
	}

}
