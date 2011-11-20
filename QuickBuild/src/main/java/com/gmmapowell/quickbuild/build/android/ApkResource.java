package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class ApkResource extends SolidResource {

	public ApkResource(Strategem parent, File apkFile) {
		super(parent, apkFile);
	}

	@Override
	public BuildResource cloneInto(CloningResource to) {
		return new ApkResource(to.getBuiltBy(), to.getClonedPath());
	}

	@Override
	public String compareAs() {
		return "Apk[" + relative + "]";
	}

	@Override
	public String toString() {
		return "apk: " + relative;
	}
}
