package com.gmmapowell.sync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class PromiseBook implements Iterable<Future<?>> {
	private List<Future<?>> promises = new ArrayList<Future<?>>();

	public <V> void add(Future<V> future) {
		promises.add(future);
	}
	
	public boolean waitForAll() {
		boolean failed = false;
		for (Future<?> f : promises) {
			try {
				f.get();
			} catch (Exception ex) {
				failed = true;
			}
		}
		return failed;
	}

	@Override
	public Iterator<Future<?>> iterator() {
		return promises.iterator();
	}
}
