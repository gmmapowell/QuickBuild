package com.gmmapowell.quickbuild.build.csharp;

import java.io.File;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class CsNature implements Nature {

	private final boolean available;
	private File vspath;

	@Override
	public boolean isAvailable() {
		return available;
	}

	public CsNature(Config cxt)
	{
		if (!cxt.hasPath("vstools"))
		{
			available = false;
			return;
		}
		vspath = cxt.getPath("vstools");
		available = true;
	}
	
	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("devenv", DevenvCommand.class);
	}

	@Override
	public void resourceAvailable(BuildResource br) {

	}

	public String getDevenv() {
		return new File(vspath, "common7/ide/devenv.exe").getPath();
	}

	@Override
	public void done() {
		
	}

	@Override
	public void info(StringBuilder sb) {
		
	}

}
