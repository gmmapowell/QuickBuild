package com.gmmapowell.system;

import java.util.Iterator;

/** Return all the files on the class path
 * 
 */
public class ClassPath implements Iterable<ClassPathResource> {
	private String glob;

	public ClassPath(String glob) {
		this.glob = glob;
	}

	public static ClassPath iterate(String glob)
	{
		return new ClassPath(glob);
	}
	
	@Override
	public Iterator<ClassPathResource> iterator() {
		return new ClassPathIterator(glob);
	}
}
