package com.gmmapowell.quickbuild.build.bash;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildContextAware;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class BashNature implements Nature, BuildContextAware {
	private BuildContext cxt;
	private final Config conf;

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("arg", ArgCommand.class);
		config.addCommandExtension("bash", BashCommand.class);
		// Command names should be scoped to super-command
		// If super-command is "null" then they are top-level commands
		config.addCommandExtension("resource1", BashResourceCommand.class);
		config.addCommandExtension("produces", BashProducesCommand.class);
	}

	public BashNature(Config conf)
	{
		this.conf = conf;
	}
	
	@Override
	public void resourceAvailable(BuildResource br) {
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
		this.cxt = cxt;
	}
}
