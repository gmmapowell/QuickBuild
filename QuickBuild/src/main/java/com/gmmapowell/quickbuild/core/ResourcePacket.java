package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.DependencyManager.ComparisonResource;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

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

	public void provide(BuildContext cxt) {
		provide(cxt, true);
	}

	public void provide(BuildContext cxt, boolean analyze) {
		for (T obj : resources)
			cxt.builtResource(obj, analyze);
	}

	@SuppressWarnings("unchecked")
	public void resolveClones() {
		loop:
		while (true) {
			for (BuildResource br : resources)
				if (br instanceof CloningResource)
				{
					CloningResource cr = (CloningResource) br;
					br = cr.getActual();
					if (br == null)
						throw new QuickBuildException("It's an error for a cloning resource to not be resolved by now");
					if (br instanceof PendingResource || br instanceof CloningResource || br instanceof ComparisonResource)
						throw new QuickBuildException("It's an error for a cloning resource to end up like this");
					if (!resources.remove(cr))
						throw new QuickBuildException("Could not remove the resource from the set");
					resources.add((T) br);
					continue loop;
				}
			break;
		}
	}
}
