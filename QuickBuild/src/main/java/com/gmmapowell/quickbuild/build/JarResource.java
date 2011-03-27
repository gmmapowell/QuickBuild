package com.gmmapowell.quickbuild.build;

import java.io.File;

public class JarResource implements BuildResource {
	private final File jarFile;

	public JarResource(File f) {
		this.jarFile = f;
	}
	
	public File getFile() {
		return jarFile;
	}

	@Override
	public String toString() {
		return "Jar["+jarFile+"]";
	}
}
