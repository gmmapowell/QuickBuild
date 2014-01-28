package com.gmmapowell.utils;

public class MutableInteger {
	private int current;

	public MutableInteger(int from) {
		this.current = from;
	}
	
	public synchronized int increment() {
		return current++;
	}
	
	public synchronized int decrement() {
		return --current;
	}
	
	public int get() {
		return current;
	}
}
