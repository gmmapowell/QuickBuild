package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.FileUtils;

public abstract class ArchiveCommand implements Tactic{

	protected File jarfile;
	protected JarResource jarResource;
	protected final List<File> dirsToJar = new ArrayList<File>();
	protected final List<File> includePackages;
	protected final List<File> excludePackages;
	private final Set <Tactic> procDeps = new HashSet<Tactic>();

	public ArchiveCommand(List<File> includePackages, List<File> excludePackages) {
		super();
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

	@Override
	public void addProcessDependency(Tactic earlier) {
		procDeps.add(earlier);
	}

	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}

}
