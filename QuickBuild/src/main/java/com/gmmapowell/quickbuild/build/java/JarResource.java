package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Tactic;
import org.zinutils.utils.FileUtils;

public class JarResource extends SolidResource {
	public JarResource(Tactic parent, File f) {
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
		return "Jar[" + FileUtils.posixPath(relative) + "]";
	}
}
