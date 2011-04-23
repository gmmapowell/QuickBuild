package com.gmmapowell.utils;

import java.io.File;
import java.util.Iterator;
import java.util.TreeSet;

public class OrderedFileList implements Iterable<File> {
	private TreeSet<File> list = new TreeSet<File>();

	public OrderedFileList(File rootdir, String string) {
		for (File f : FileUtils.findFilesMatching(rootdir, string))
			list.add(f);
	}

	@Override
	public Iterator<File> iterator() {
		return list.iterator();
	}

}
