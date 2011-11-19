package com.gmmapowell.quickbuild.build.bash;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildContextAware;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class BashNature implements Nature, BuildContextAware {

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("arg", ArgCommand.class);
		config.addCommandExtension("bash", BashCommand.class);
		// Command names should be scoped to super-command
		// If super-command is "null" then they are top-level commands
		config.addCommandExtension("dir", BashDirectoryCommand.class);
	}

	public BashNature(Config conf)
	{
	}
	
	@Override
	public void resourceAvailable(BuildResource br, boolean analyze) {
	}

	public boolean isAvailable() {
		return true;
	}

	@Override
	public void done() {
	}

	@Override
	public void info(StringBuilder sb) {
	}

	@Override
	public void provideBuildContext(BuildContext cxt) {
	}
}
