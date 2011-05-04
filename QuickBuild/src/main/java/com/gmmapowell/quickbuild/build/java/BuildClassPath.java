package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.utils.PathBuilder;

public class BuildClassPath {
	private Set<File> files = new HashSet<File>();

	public void add(File file) {
		files.add(file);
	}

	public String toString()
	{
		PathBuilder pb = new PathBuilder();
		toString(pb);
		return pb.toString();
	}

	protected void toString(PathBuilder pb) {
		for (File f : files)
			pb.add(f);
	}

	public boolean empty() {
		return files.isEmpty();
	}
}