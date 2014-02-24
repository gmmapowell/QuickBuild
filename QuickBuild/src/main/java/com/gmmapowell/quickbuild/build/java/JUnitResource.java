package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.FileUtils;

public class JUnitResource  extends SolidResource {
	public JUnitResource(Tactic parent, File f) {
		super(parent, f);
	}
	
	public File getFile() {
		return relative;
	}

	@Override
	public String toString() {
		return compareAs();
	}


	@Override
	public String compareAs() {
		return "JUnt[" + FileUtils.posixPath(relative) + "]";
	}
}
