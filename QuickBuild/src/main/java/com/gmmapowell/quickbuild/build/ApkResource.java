package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class ApkResource implements BuildResource {

	private final File apkFile;
	private final Strategem parent;

	public ApkResource(Strategem parent, File apkFile) {
		this.parent = parent;
		this.apkFile = apkFile;
	}

	@Override
	public Strategem getBuiltBy() {
		return parent;
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
		if (obj == null)
			return false;
		return toString().equals(obj.toString());
	}
	
	@Override
	public String toString() {
		return "apk: " + apkFile;
	}

	public File getPath() {
		// TODO Auto-generated method stub
		return null;
	}
}
