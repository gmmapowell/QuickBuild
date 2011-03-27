package com.gmmapowell.quickbuild.config;

import java.io.File;

public class Project {

	private final File basedir;

	public Project(File projdir) {
		this.basedir = projdir;
	}

	public File getBaseDir() {
		return basedir;
	}

}
