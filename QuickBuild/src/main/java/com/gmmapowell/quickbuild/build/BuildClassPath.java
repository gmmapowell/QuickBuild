package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.utils.PathBuilder;

public class BuildClassPath {
	private List<File> files = new ArrayList<File>();

	public void add(File file) {
		files.add(file);
	}

	public String toString()
	{
		PathBuilder pb = new PathBuilder();
		for (File f : files)
			pb.add(f);
		return pb.toString();
	}
}
