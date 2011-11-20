package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class ApkResource extends SolidResource {

	private final String comparison;

	public ApkResource(Strategem parent, File apkFile) {
		super(parent, apkFile);
		comparison = "Apk[" + relative + "]";
	}

	@Override
	public BuildResource cloneInto(CloningResource to) {
		return new ApkResource(to.getBuiltBy(), to.getClonedPath());
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
		return ((obj instanceof ApkResource) && comparison.equals(((ApkResource)obj).comparison));
	}

	@Override
	public String toString() {
		return "apk: " + relative;
	}
}
