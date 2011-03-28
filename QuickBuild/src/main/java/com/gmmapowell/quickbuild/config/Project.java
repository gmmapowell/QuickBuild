package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildResource;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

public class Project implements BuildResource {
	private final String name;
	private final File basedir;
	private final File outdir;
	private final String nature;

	public Project(String nature, String name, File projdir, String output) {
		this.nature = nature;
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
	
	
	// TODO: because we use this to identify projects, it needs to be unique.  Currently this is not
	// enough.  We need something more, from arguments (eg. the jar file we're building) to unique ids
	// or SHA1s.
	@Override
	public String toString() {
		return "Project["+nature+"-"+name+"]";
	}

	public File getRelative(String dir) {
		return new File(basedir, dir);
	}

	public File makeRelative(File f)
	{
		return FileUtils.makeRelativeTo(f, basedir);
	}
	
	public File getOutput(String dir) {
		return new File(outdir, dir);
	}

	@Override
	public Project getBuiltBy() {
		throw new QuickBuildException("You cannot ask who built a project.  You did something wrong");
	}

}
