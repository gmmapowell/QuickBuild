package com.gmmapowell.quickbuild.build.deployment;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class DeployNature implements Nature {

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("deploy", DeployCommand.class);
		config.addCommandExtension("after", AfterCommand.class);
	}

	public DeployNature(Config cxt)
	{
	}

	@Override
	public void resourceAvailable(BuildResource br, boolean analyze) {

	}

	public boolean isAvailable() {
		return false;
	}

	@Override
	public void done() {
		
	}

	@Override
	public void info(StringBuilder sb) {
		
	}

}
