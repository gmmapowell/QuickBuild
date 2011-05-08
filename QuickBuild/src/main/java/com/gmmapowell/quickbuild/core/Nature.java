package com.gmmapowell.quickbuild.core;

public interface Nature {

	void resourceAvailable(BuildResource br);

	public abstract boolean isAvailable();

	void done();

	void info(StringBuilder sb);
}
