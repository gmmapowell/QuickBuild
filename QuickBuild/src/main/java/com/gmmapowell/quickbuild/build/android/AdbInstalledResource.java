package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Tactic;

public class AdbInstalledResource implements BuildResource {
	private final Tactic tactic;
	private final String comparison;
	private boolean analyze;

	public AdbInstalledResource(Tactic t, String apk) {
		this.tactic = t;
		comparison = "AdbInstalled["+apk.replaceAll("\\.", "_").replaceAll("/", "_")+"]";
	}

	@Override
	public Tactic getBuiltBy() {
		return tactic;
	}

	@Override
	public File getPath() {
		return null;
	}

	@Override
	public List<File> getPaths() {
		return new ArrayList<>();
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
	public void enableAnalysis() {
		analyze = true;
	}

	@Override
	public boolean doAnalysis() {
		return analyze;
	}

	@Override
	public int compareTo(BuildResource o) {
		return toString().compareTo(o.toString());
	}

	@Override
	public String toString() {
		return compareAs();
	}
}
