package com.gmmapowell.utils;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class OrderedFileList implements Iterable<File> {
	private TreeSet<File> list = new TreeSet<File>();

	public OrderedFileList(File rootdir, String string) {
		add(rootdir, string);
	}

	public OrderedFileList(File... files) {
		add(files);
	}

	public OrderedFileList(List<File> files) {
		list.addAll(files); 
	}

	public void add(OrderedFileList other) {
		list.addAll(other.list);
	}

	public void add(File rootdir, String string) {
		for (File f : FileUtils.findFilesMatching(rootdir, string))
			if (f.isFile())
				list.add(f);
	}
	
	public void add(File... files) {
		for (File f : files)
			list.add(f);
	}

	public void remove(File f) {
		list.remove(f);
	}

	@Override
	public Iterator<File> iterator() {
		return list.iterator();
	}

	public boolean isEmpty()
	{
		return list.size() == 0;
	}
	
	public static OrderedFileList empty() {
		return new OrderedFileList();
	}
	
	@Override
	public String toString() {
		return list.toString();
	}
}
