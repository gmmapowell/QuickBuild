package com.gmmapowell.sync;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.gmmapowell.exceptions.UtilException;

public class Promise<T> implements Future<T>, RecoveryFutureCommon<T> {
	private boolean done;
	private T obj;
	private Throwable error;
	private Handler<T> then;

	public Promise() {
	}

	public Promise(T obj) {
		this.obj = obj;
		done = true;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	public Promise<T> then(Handler<T> then) {
		this.then = then;
		return this;
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public T get() {
		try {
			synchronized (this) {
				if (done)
					return obj;
				this.wait();
			}
			return obj;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	// This is here while I work through things ... should be removed in the long run
	public T lazyget() {
		return get();
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		synchronized (this) {
			if (done)
				return obj;
			this.wait(unit.toMillis(timeout));
			if (!done)
				throw new TimeoutException();
		}
		return obj;
	}

	public synchronized void completed(T object) {
		this.obj = object;
		this.done = true;
		this.notifyAll();
		if (then != null)
			then.handle(object);
	}

	@Override
	public void recoverFrom(T obj) {
		completed(obj);
	}

	@Override
	public synchronized void failed(Throwable t) {
		this.error = t;
		this.done = true;
		this.notifyAll();
		if (then != null)
			then.failed(t);
	}
}
