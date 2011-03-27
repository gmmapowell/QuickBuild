package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildResource;

public class Project implements BuildResource {
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
	
	@Override
	public String toString() {
		return "Project["+name+"]";
	}

}
