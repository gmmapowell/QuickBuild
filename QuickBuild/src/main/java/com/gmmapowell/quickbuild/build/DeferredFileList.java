package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.zinutils.utils.FileUtils;

public class DeferredFileList implements List<File> {
	private List<File> delegate;
	private final File gendir;
	private final String pattern;
	
	public DeferredFileList(File gendir, String pattern) {
		this.gendir = gendir;
		this.pattern = pattern;
	}

	private void init()
	{
		if (delegate == null)
		{
			if (gendir.isDirectory())
				delegate = FileUtils.findFilesMatching(gendir, pattern);
			else
				delegate = new ArrayList<File>();
		}
	}

	public int size() {
		init();
		return delegate.size();
	}

	public boolean isEmpty() {
		init();
		return delegate.isEmpty();
	}

	public boolean contains(Object o) {
		init();
		return delegate.contains(o);
	}

	public int indexOf(Object o) {
		init();
		return delegate.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		init();
		return delegate.lastIndexOf(o);
	}

	public Iterator<File> iterator() {
		init();
		return delegate.iterator();
	}

	public boolean containsAll(Collection<?> c) {
		init();
		return delegate.containsAll(c);
	}

	public ListIterator<File> listIterator() {
		init();
		return delegate.listIterator();
	}

	public Object[] toArray() {
		init();
		return delegate.toArray();
	}

	public ListIterator<File> listIterator(int index) {
		init();
		return delegate.listIterator(index);
	}

	public <T> T[] toArray(T[] a) {
		init();
		return delegate.toArray(a);
	}

	public boolean removeAll(Collection<?> c) {
		init();
		return delegate.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		init();
		return delegate.retainAll(c);
	}

	public File get(int index) {
		init();
		return delegate.get(index);
	}

	public File set(int index, File element) {
		init();
		return delegate.set(index, element);
	}

	public boolean add(File e) {
		init();
		return delegate.add(e);
	}

	public void add(int index, File element) {
		init();
		delegate.add(index, element);
	}

	public String toString() {
		init();
		return delegate.toString();
	}

	public List<File> subList(int fromIndex, int toIndex) {
		init();
		return delegate.subList(fromIndex, toIndex);
	}

	public File remove(int index) {
		init();
		return delegate.remove(index);
	}

	public boolean remove(Object o) {
		init();
		return delegate.remove(o);
	}

	public boolean equals(Object o) {
		init();
		return delegate.equals(o);
	}

	public void clear() {
		init();
		delegate.clear();
	}

	public boolean addAll(Collection<? extends File> c) {
		init();
		return delegate.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends File> c) {
		init();
		return delegate.addAll(index, c);
	}

	public int hashCode() {
		init();
		return delegate.hashCode();
	}
}
