package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.Collection;

import com.gmmapowell.quickbuild.build.BuildCommand;

public interface ConfigBuildCommand extends ConfigCommand {

	Collection<? extends BuildCommand> buildCommands(Config conf, Project proj);
	File projectDir();

}
