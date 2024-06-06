package com.gmmapowell.utils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.zinutils.utils.FileUtils;

public class OrderedFileList implements Iterable<File> {
	private TreeSet<File> list = new TreeSet<File>();

	public OrderedFileList(File rootdir, String string) {
		add(rootdir, string);
	}

	public OrderedFileList(File... files) {
		add(files);
	}

	public OrderedFileList(List<File> files) {
		for (File f : files)
			addOne(f);
	}

	public void add(OrderedFileList other) {
		list.addAll(other.list);
	}

	public void add(File rootdir, String string) {
		for (File f : FileUtils.findFilesMatching(rootdir, string))
			addOne(f);
	}
	
	public void add(List<File> files) {
		for (File f : files)
			addOne(f);
	}

	public void add(File... files) {
		for (File f : files)
			addOne(f);
	}

	private void addOne(File f) {
		try {
			list.add(f.getCanonicalFile());
		} catch (IOException e) {
			// file doesn't exist - can't add it!
		}
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
