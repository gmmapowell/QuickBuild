package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.utils.FileUtils;

public class ApkResource extends SolidResource {

	private final String comparison;

	public ApkResource(Strategem parent, File apkFile) {
		super(parent, apkFile);
		comparison = "Apk[" + FileUtils.posixPath(relative) + "]";
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
	public String toString() {
		return comparison;
	}
}
