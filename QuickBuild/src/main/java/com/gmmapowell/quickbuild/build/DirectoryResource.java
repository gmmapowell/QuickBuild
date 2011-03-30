package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.utils.FileUtils;

public class DirectoryResource implements BuildResource {
	private final Project builder;
	private final File dir;

	public DirectoryResource(Project builder, File dir) {
		this.builder = builder;
		this.dir = FileUtils.relativePath(dir);
	}

	@Override
	public Project getBuiltBy() {
		return builder;
	}

	@Override
	public boolean equals(Object obj) {
		return this.toString().equals(obj.toString());
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return "Directory: " + FileUtils.makeRelative(dir);
	}
}
