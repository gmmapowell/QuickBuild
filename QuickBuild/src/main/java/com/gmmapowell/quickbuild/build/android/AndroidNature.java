package com.gmmapowell.quickbuild.build.android;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class AndroidNature implements Nature {
	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("android", AndroidCommand.class);
		config.addCommandExtension("use", AndroidUseLibraryCommand.class);
		config.addCommandExtension("android-jar", AndroidJarCommand.class);
		config.addCommandExtension("adbinstall", AdbInstallCommand.class);
	}

	public AndroidNature(Config cxt)
	{
	}
	
	@Override
	public void resourceAvailable(BuildResource br) {
		throw new UtilException("Can't handle " + br);
	}

	public boolean isAvailable() {
		return true;
	}

	@Override
	public void done() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(StringBuilder sb) {
		// TODO Auto-generated method stub
		
	}
}
