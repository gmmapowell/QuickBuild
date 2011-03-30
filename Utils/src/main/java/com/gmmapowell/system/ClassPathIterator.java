package com.gmmapowell.system;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.lambda.Lambda;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.GPJarEntry;
import com.gmmapowell.utils.GPJarFile;

public class ClassPathIterator implements Iterator<ClassPathResource> {
	private String glob;
	private Iterator<File> itemIterator;
	private Iterator<ClassPathResource> entryIterator;

	public ClassPathIterator(String glob) {
		this.glob = glob;
		String[] elts = System.getProperty("java.class.path").split(";");
		ArrayList<File> items = new ArrayList<File>();
		for (String s : elts)
		{
			items.add(new File(s));
		}
		itemIterator = items.iterator();
	}

	@Override
	public boolean hasNext() {
		if (entryIterator != null && entryIterator.hasNext())
			return true;

		// Move on to the next entry and see what it has
		while (itemIterator.hasNext())
		{
			File from = itemIterator.next();
			if (from.isFile())
			{
				GPJarFile jf = new GPJarFile(from);
				entryIterator = Lambda.map(ClassPathResource.fromJar, Lambda.filter(GPJarEntry.nameMatches(glob), jf.iterator()));
				if (entryIterator.hasNext())
					return true;
			}
			else
			{
				entryIterator = Lambda.map(ClassPathResource.fromFile, FileUtils.findFilesMatching(from, glob).iterator());
				if (entryIterator.hasNext())
					return true;
			}
		}
		return false;
	}

	@Override
	public ClassPathResource next() {
		if (!hasNext())
			throw new UtilException("There are no more entries");
		return entryIterator.next();
	}

	@Override
	public void remove() {
		throw new UtilException("The remove operation is not supported on ClassPathIterator");
	}

}
