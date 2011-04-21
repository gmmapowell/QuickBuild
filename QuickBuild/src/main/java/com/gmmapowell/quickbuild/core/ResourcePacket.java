package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ResourcePacket implements Iterable<BuildResource> {
	private Set<BuildResource> resources = new HashSet<BuildResource>();
	
	@Override
	public Iterator<BuildResource> iterator() {
		return resources.iterator();
	}

	public void add(BuildResource resource) {
		resources.add(resource);
	}

}
