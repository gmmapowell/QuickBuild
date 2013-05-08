package com.gmmapowell.sync;

public interface Handler<T> {
	void handle(T obj);
	void failed(Throwable t);
}
