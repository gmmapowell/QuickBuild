package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class AdbInstalledResource implements BuildResource {
	private final AdbInstallCommand strat;
	private final String comparison;
	private boolean analyze;

	public AdbInstalledResource(AdbInstallCommand strat, String apk) {
		this.strat = strat;
		comparison = "AdbInstalled["+apk.replaceAll("\\.", "_").replaceAll("/", "_")+"]";
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
		return comparison;
	}

	@Override
	public int hashCode() {
		return comparison.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return ((obj instanceof AdbInstalledResource) && comparison.equals(((AdbInstalledResource)obj).comparison));
	}
	
	@Override
	public BuildResource cloneInto(CloningResource toResource) {
		throw new RuntimeException("I don't really understand this");
	}
	
	@Override
	public void enableAnalysis() {
		analyze = true;
	}

	@Override
	public boolean doAnalysis() {
		return analyze;
	}

	@Override
	public String toString() {
		return compareAs();
	}
}
