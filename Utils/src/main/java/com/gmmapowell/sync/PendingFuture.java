package com.gmmapowell.sync;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PendingFuture<T> implements Future<T>, RecoveryFutureCommon<T> {
	private boolean done;
	private T obj;
	private Throwable error;

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		synchronized (this) {
			if (done)
				return obj;
			this.wait();
		}
		return obj;
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
	}

}
