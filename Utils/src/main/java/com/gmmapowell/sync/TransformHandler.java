package com.gmmapowell.sync;

public interface TransformHandler<T,V> extends Handler<T> {
	void sendTo(Promise<V> obj);
}
