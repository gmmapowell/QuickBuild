package com.gmmapowell.quickbuild.build.android;

import org.zinutils.exceptions.UtilException;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class AndroidNature implements Nature {
	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("android", AndroidCommand.class);
		config.addCommandExtension("use", AndroidUseLibraryCommand.class);
		config.addCommandExtension("espresso", AndroidEspressoTestsCommand.class);
		config.addCommandExtension("exportJar", AndroidExportJarCommand.class);
		config.addCommandExtension("android-jar", AndroidJarCommand.class);
		config.addCommandExtension("adbinstall", AdbInstallCommand.class);
		config.addCommandExtension("adbstart", AdbStartCommand.class);
		config.addCommandExtension("adbinstrument", AdbInstrumentCommand.class);
		config.addCommandExtension("jniso", AndroidUseJNICommand.class);
	}

	public AndroidNature(Config cxt)
	{
	}
	
	@Override
	public void resourceAvailable(BuildResource br, boolean analyze) {
		throw new UtilException("Can't handle " + br);
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
}
