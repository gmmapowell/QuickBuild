package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class ApkResource extends SolidResource {

	public ApkResource(Strategem parent, File apkFile) {
		super(parent, apkFile);
	}

	@Override
	public String compareAs() {
		return "Apk[" + file + "]";
	}

	@Override
	public String toString() {
		return "apk: " + file;
	}
}
