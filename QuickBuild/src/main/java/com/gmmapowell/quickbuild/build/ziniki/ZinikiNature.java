package com.gmmapowell.quickbuild.build.ziniki;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class ZinikiNature implements Nature {

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("ziniki", ZinikiCommand.class);
		config.addCommandExtension("privileged", ZinikiModeCommand.class);
	}

	public ZinikiNature(Config conf)
	{
//		this.conf = conf;
//		File libdir = conf.getQuickBuildDir();
//		if (libdir == null)
//			libdir = FileUtils.getCurrentDir();
//		libdirs.add(new LibDir(new File(libdir, "libs"), new ArrayList<ExcludeCommand>()));
	}

	@Override
	public void resourceAvailable(BuildResource br, boolean analyze) {
		// TODO Auto-generated method stub
		
	}

	@Override
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
