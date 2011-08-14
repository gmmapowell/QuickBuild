package com.gmmapowell.quickbuild.core;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class CloningResource implements BuildResource {
	private final File clonedPath;
	private final Strategem builtBy;
	private BuildResource actual;
	private final PendingResource pending;
	private final int hash;

	public CloningResource(Strategem builtBy, PendingResource fromResource, File clonedPath) {
		this.builtBy = builtBy;
		this.pending = fromResource;
		this.clonedPath = clonedPath;
		this.hash = pending.hashCode();
	}

	public PendingResource getPending() {
		return pending;
	}

	public File getClonedPath() {
		return clonedPath;
	}

	public void bind(BuildResource actual) {
		this.actual = actual;
	}
	
	public BuildResource getActual() {
		return actual;
	}
	
	@Override
	public File getPath() {
		throw new QuickBuildException("Just don't use it like this");
	}

	@Override
	public String compareAs() {
		throw new QuickBuildException("Just don't use it like this");
	}

	@Override
	public Strategem getBuiltBy() {
		return builtBy;
	}

	@Override
	public BuildResource cloneInto(CloningResource to) {
		throw new UtilException("Cannot clone a cloning resource");
	}
	
	/*
	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof CloningResource))
			return false;
		return hash == obj.hashCode();
	}
	*/
	@Override
	public String toString() {
		return "CloneTo[" + clonedPath + (actual != null ? "*": "") + "]";
	}
}
