package com.gmmapowell.quickbuild.config;

import java.io.File;

public class Project {
	private final String name;
	private final File basedir;
	private final File outdir;

	public Project(String name, File projdir, String output) {
		this.name = name;
		this.basedir = projdir;
		this.outdir = new File(projdir, output);
	}

	public File getBaseDir() {
		return basedir;
	}

	public File getOutputDir() {
		return outdir;
	}

	public String getName() {
		return name;
	}

}
