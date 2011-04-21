package com.gmmapowell.quickbuild.config;

import com.gmmapowell.quickbuild.core.Strategem;

public interface ConfigBuildCommand extends ConfigCommand {

	Strategem applyConfig(Config config);

}
