package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.utils.FileUtils;

public abstract class ArchiveCommand extends AbstractTactic {
	protected File jarfile;
	protected JarResource jarResource;
	protected final List<File> dirsToJar = new ArrayList<File>();
	protected final List<File> includePackages;
	protected final List<File> excludePackages;

	public ArchiveCommand(Strategem parent, List<File> includePackages, List<File> excludePackages) {
		super(parent);
		this.includePackages = includePackages;
		this.excludePackages = excludePackages;
	}

	public void add(File file) {
		dirsToJar.add(file);
	}

	public File getFile() {
		return FileUtils.makeRelative(jarfile);
	}

	public JarResource getJarResource() {
		return jarResource;
	}
}
