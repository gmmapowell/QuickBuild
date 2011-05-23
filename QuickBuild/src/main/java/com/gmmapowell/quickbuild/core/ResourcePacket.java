package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ResourcePacket<T extends BuildResource> implements Iterable<T> {
	private Set<T> resources = new HashSet<T>();
	
	@Override
	public Iterator<T> iterator() {
		return resources.iterator();
	}

	public void add(T resource) {
		resources.add(resource);
	}
	
	@Override
	public String toString() {
		return resources.toString();
	}

}
