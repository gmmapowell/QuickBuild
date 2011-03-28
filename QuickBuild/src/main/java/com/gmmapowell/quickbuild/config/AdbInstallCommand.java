package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.AdbCommand;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class AdbInstallCommand extends NoChildCommand implements ConfigBuildCommand {
	private String projectName;
	private final File projectDir;
	private Project project;
	private AndroidContext acxt;

	public AdbInstallCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public void applyConfig(Config config) {
		project = new Project("adb", projectName, projectDir, config.getOutput());
		acxt = config.getAndroidContext();
	}
	
	@Override
	public Project project() {
		return project;
	}

	@Override
	public Collection<? extends BuildCommand> buildCommands() {
		List<BuildCommand> ret = new ArrayList<BuildCommand>();
		AdbCommand cmd = new AdbCommand(acxt, project);
		cmd.reinstall();
		ret.add(cmd);
		return ret;
	}

}
