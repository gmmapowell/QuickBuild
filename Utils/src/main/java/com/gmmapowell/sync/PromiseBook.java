package com.gmmapowell.sync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;

public class PromiseBook implements Iterable<Future<?>> {
	private final Logger logger;
	private final List<Future<?>> promises = new ArrayList<Future<?>>();
	
	public PromiseBook(Logger logger) {
		this.logger = logger;
	}

	public <V> Promise<V> newPromise() {
		Promise<V> ret = new Promise<V>();
		add(ret);
		return ret;
	}
	
	public <V> void add(Future<V> future) {
		promises.add(future);
	}
	
	public boolean waitForAll() {
		boolean failed = false;
		for (Future<?> f : promises) {
			try {
				f.get(100, TimeUnit.MILLISECONDS);
			} catch (TimeoutException ex) {
				logger.error("Timed out waiting for promise " + f);
			} catch (Exception ex) {
				logger.error("Failed to get promise " + f, ex);
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
