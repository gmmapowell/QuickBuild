package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.zinutils.utils.PathBuilder;

public class BuildClassPath implements Iterable<File> {
	private Set<File> files = new HashSet<File>();

	public void add(File file) {
		if (file == null)
			return;
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

	public boolean contains(File file) {
		return files.contains(file);
	}

	public boolean empty() {
		return files.isEmpty();
	}

	@Override
	public Iterator<File> iterator() {
		return files.iterator();
	}
}
