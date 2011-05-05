package com.gmmapowell.quickbuild.build.android;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class AndroidNature implements Nature {
	public AndroidNature(BuildContext cxt)
	{
	}
	
	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("android", AndroidCommand.class);
		config.addCommandExtension("use", AndroidUseLibraryCommand.class);
		config.addCommandExtension("android-jar", AndroidJarCommand.class);
		config.addCommandExtension("adbinstall", AdbInstallCommand.class);
	}

	@Override
	public void resourceAvailable(BuildResource br) {
		throw new UtilException("Can't handle " + br);
	}

	public boolean isAvailable() {
		return true;
	}
}
