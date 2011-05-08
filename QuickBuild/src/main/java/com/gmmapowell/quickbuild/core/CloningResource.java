package com.gmmapowell.quickbuild.core;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;

public class CloningResource implements BuildResource {
	private final File clonedPath;
	private final Strategem builtBy;
	private BuildResource clonedAs;

	public CloningResource(Strategem builtBy, File clonedPath) {
		this.builtBy = builtBy;
		this.clonedPath = clonedPath;
	}
	
	@Override
	public File getPath() {
		throw new UtilException("Cannot use a CloningResource without instantiating it");
	}

	@Override
	public String compareAs() {
		throw new UtilException("Cannot use a cloning resource as a matching one");
	}

	public File getClonedPath() {
		return clonedPath;
	}

	@Override
	public String toString() {
		return "CloneTo[" + clonedPath + "]";
	}

	@Override
	public Strategem getBuiltBy() {
		return builtBy;
	}

	@Override
	public BuildResource cloneInto(CloningResource to) {
		throw new UtilException("Cannot clone a cloning resource");
	}

	public BuildResource clonedAs() {
		return clonedAs;
	}

	public void wasClonedAs(BuildResource actualTo) {
		clonedAs = actualTo;
	}
}
