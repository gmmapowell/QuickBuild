package com.gmmapowell.quickbuild.build.maven;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class MavenNature implements Nature {
	public MavenNature(BuildContext cxt)
	{
	}

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("repo", RepoCommand.class);
		config.addCommandExtension("maven", MavenLibraryCommand.class);
	}
	
	@Override
	public void resourceAvailable(BuildResource br) {
		throw new UtilException("Can't handle " + br);
	}
}
