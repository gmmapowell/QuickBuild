package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Tactic;

public class AdbInstrumentedResource implements BuildResource {
	private final Tactic tactic;
	private final String comparison;
	private boolean analyze;

	public AdbInstrumentedResource(Tactic t, String apk) {
		this.tactic = t;
		comparison = "AdbInstrumented["+apk.replaceAll("\\.", "_").replaceAll("/", "_")+"]";
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
		return ((obj instanceof AdbInstrumentedResource) && comparison.equals(((AdbInstrumentedResource)obj).comparison));
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
