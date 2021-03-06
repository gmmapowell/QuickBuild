package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Tactic;
import org.zinutils.utils.FileUtils;

public class ApkResource extends SolidResource {

	private final String comparison;

	public ApkResource(Tactic parent, File apkFile) {
		super(parent, apkFile);
		comparison = "Apk[" + FileUtils.posixPath(relative) + "]";
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
