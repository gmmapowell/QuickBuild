package com.gmmapowell.quickbuild.build.csharp;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class CsNature implements Nature {

	private final boolean available;
	private File vspath;

	public CsNature(BuildContext cxt)
	{
		if (!cxt.getConfig().hasPath("vstools"))
		{
			available = false;
			return;
		}
		vspath = cxt.getConfig().getPath("vstools");
		available = true;
	}
	
	@Override
	public boolean isAvailable() {
		return available;
	}

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("devenv", DevenvCommand.class);
	}

	@Override
	public void resourceAvailable(BuildResource br) {
		// TODO Auto-generated method stub

	}

	public String getDevenv() {
		return new File(vspath, "common7/ide/devenv.exe").getPath();
	}

}
