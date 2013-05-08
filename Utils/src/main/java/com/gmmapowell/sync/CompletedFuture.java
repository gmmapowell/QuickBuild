package com.gmmapowell.sync;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletedFuture<T> implements Future<T> {

	private final T ret;

	public CompletedFuture(T ret) {
		this.ret = ret;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return ret;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return ret;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}
}
