package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.utils.FileUtils;

public class JarResource implements BuildResource {
	private final File jarFile;
	private final Project builtBy;

	public JarResource(File f, Project builtBy) {
		this.jarFile = f;
		this.builtBy = builtBy;
	}
	
	public Project getBuiltBy() {
		return builtBy;
	}
	
	public File getFile() {
		return FileUtils.makeRelative(jarFile);
	}

	@Override
	public String toString() {
		return "Jar["+FileUtils.makeRelative(jarFile)+"]";
	}
}
