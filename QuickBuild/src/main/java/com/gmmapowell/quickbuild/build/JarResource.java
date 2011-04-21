package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.utils.FileUtils;

public class JarResource implements BuildResource {
	private final File jarFile;
	private final Strategem builtBy;

	public JarResource(File f, Strategem parent) {
		this.jarFile = f;
		this.builtBy = parent;
	}
	
	
	public File getFile() {
		return FileUtils.makeRelative(jarFile);
	}

	@Override
	public String toString() {
		return "Jar["+FileUtils.makeRelative(jarFile)+"]";
	}


	@Override
	public Strategem getBuiltBy() {
		return builtBy;
	}


	@Override
	public File getPath() {
		return jarFile;
	}
}
