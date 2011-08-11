package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class AdbInstalledResource implements BuildResource {
	private final AdbInstallCommand strat;
	private final String apk;

	public AdbInstalledResource(AdbInstallCommand strat, String apk) {
		this.strat = strat;
		this.apk = apk;
	}

	@Override
	public Strategem getBuiltBy() {
		return strat;
	}

	@Override
	public File getPath() {
		return null;
	}

	@Override
	public String compareAs() {
		return "AdbInstalled["+apk.replaceAll("\\.", "_").replaceAll("/", "_")+"]";
	}

	@Override
	public BuildResource cloneInto(CloningResource toResource) {
		throw new RuntimeException("I don't really understand this");
	}
	
	@Override
	public String toString() {
		return compareAs();
	}
}
