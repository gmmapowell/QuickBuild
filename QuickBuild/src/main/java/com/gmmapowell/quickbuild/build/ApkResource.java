package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.quickbuild.config.Project;

public class ApkResource implements BuildResource {

	private final Project builder;
	private final File apkFile;

	public ApkResource(Project builder, File apkFile) {
		this.builder = builder;
		this.apkFile = apkFile;
	}

	@Override
	public Project getBuiltBy() {
		return builder;
	}

	public File getFile() {
		return apkFile;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return toString().equals(obj.toString());
	}
	
	@Override
	public String toString() {
		return "apk: " + apkFile;
	}
}
