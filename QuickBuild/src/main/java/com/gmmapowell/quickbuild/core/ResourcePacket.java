package com.gmmapowell.quickbuild.core;

import java.util.ArrayList;
import java.util.Iterator;

public class ResourcePacket implements Iterable<BuildResource> {

	@Override
	public Iterator<BuildResource> iterator() {
		
		// default case: return a blank list
		return new ArrayList<BuildResource>().iterator();
	}

}
