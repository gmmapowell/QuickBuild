package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.Strategem;

import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;

public abstract class ArchiveCommand extends AbstractTactic {
	protected File jarfile;
	protected JarResource jarResource;
	protected final List<File> dirsToJar = new ArrayList<File>();
	protected final List<File> includePackages;
	protected final List<File> excludePackages;
	private final OrderedFileList resourceFiles;

	public ArchiveCommand(Strategem parent, List<File> includePackages, List<File> excludePackages, OrderedFileList resourceFiles) {
		super(parent);
		this.includePackages = includePackages;
		this.excludePackages = excludePackages;
		this.resourceFiles = resourceFiles;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return resourceFiles;
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

	public boolean alwaysBuild() {
		return false;
	}
}
