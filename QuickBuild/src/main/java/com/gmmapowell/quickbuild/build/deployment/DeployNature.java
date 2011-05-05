package com.gmmapowell.quickbuild.build.deployment;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class DeployNature implements Nature {

	public DeployNature(BuildContext cxt)
	{
	}

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("deploy", DeployCommand.class);
	}


	@Override
	public void resourceAvailable(BuildResource br) {
		// TODO Auto-generated method stub

	}

	public boolean isAvailable() {
		return false;
	}

}
