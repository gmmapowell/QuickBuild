package com.gmmapowell.quickbuild.core;

import java.io.File;

import com.gmmapowell.utils.FileUtils;

public class StructureHelper {
	private final File basedir;
	private final File outdir;

	public StructureHelper(File basedir, String output) {
		this.basedir = basedir;
		this.outdir = new File(basedir, output);
	}
	
	public File getBaseDir() {
		return basedir;
	}

	public File getOutputDir() {
		return outdir;
	}

	public File getRelative(String dir) {
		return new File(basedir, dir);
	}

	public File makeRelative(File f)
	{
		return FileUtils.makeRelativeTo(f, basedir);
	}
	
	public File getOutput(String path) {
		return new File(outdir, path);
	}
}
