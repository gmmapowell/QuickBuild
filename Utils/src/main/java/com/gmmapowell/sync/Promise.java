package com.gmmapowell.sync;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.gmmapowell.exceptions.UtilException;

public class Promise<T> implements Future<T> {
	enum Outcome { PENDING, SUCCESS, FAILURE };
	private Outcome done;
	private T obj;
	private Throwable error;
	private final Set<Handler<T>> then = new HashSet<Handler<T>>();

	public Promise() {
		done = Outcome.PENDING;
	}

	public Promise(T obj) {
		this.obj = obj;
		done = Outcome.SUCCESS;
	}

	public Promise(Throwable error) {
		this.error = error;
		done = Outcome.FAILURE;
	}

	@Override
	public boolean isDone() {
		return done != Outcome.PENDING;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	public <V> Promise<V> transform(TransformHandler<T,V> handler) {
		Promise<V> ret = new Promise<V>();
		handler.sendTo(ret);
		this.then.add(handler);
		return ret;
	}

	public Promise<T> then(Handler<T> then) {
		if (done == Outcome.SUCCESS)
			then.handle(obj);
		else if (done == Outcome.FAILURE)
			then.failed(error);
		else {
			this.then.add(then);
		}
		return this;
	}
	
	public Promise<T> tell(final Promise<T> other) {
		this.then(new Handler<T>() {
			@Override
			public void handle(T obj) {
				other.completed(obj);
			}

			@Override
			public void failed(Throwable t) {
				other.failed(t);
			}
		});
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
				while (done == Outcome.PENDING)
					this.wait();

				if (done == Outcome.FAILURE)
					throw UtilException.wrap(error);
				else if (done == Outcome.SUCCESS)
					return obj;
				else
					throw new TimeoutException();
			}
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
			if (done == Outcome.PENDING)
				this.wait(unit.toMillis(timeout));

			if (done == Outcome.FAILURE)
				throw UtilException.wrap(error);
			else if (done == Outcome.SUCCESS)
				return obj;
			else
				throw new TimeoutException();
		}
	}

	public synchronized void completed(T object) {
		this.obj = object;
		this.done = Outcome.SUCCESS;
		this.notifyAll();
		for (Handler<T> h : then)
			h.handle(object);
	}

	public synchronized void failed(Throwable t) {
		this.error = t;
		this.done = Outcome.FAILURE;
		this.notifyAll();
		for (Handler<T> h : then)
			h.failed(t);
	}
}
