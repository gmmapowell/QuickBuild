package com.gmmapowell.quickbuild.core;

public interface Nature {

	void resourceAvailable(BuildResource br, boolean analyze);

	public abstract boolean isAvailable();

	void done();

	void info(StringBuilder sb);
}
