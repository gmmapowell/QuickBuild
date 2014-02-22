package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.quickbuild.build.ziniki.ZinikiCommand;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;

public class ZinikiNature implements Nature {

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("ziniki", ZinikiCommand.class);
	}
	
	@Override
	public void resourceAvailable(BuildResource br, boolean analyze) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isAvailable() {
		// TODO Auto-generated method stub
		return false;
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
