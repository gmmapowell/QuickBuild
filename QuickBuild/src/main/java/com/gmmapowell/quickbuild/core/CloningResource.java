package com.gmmapowell.quickbuild.core;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class CloningResource implements BuildResource {
	private final File clonedPath;
	private final Strategem builtBy;
	private BuildResource actual;
	private final PendingResource pending;

	public CloningResource(Strategem builtBy, PendingResource fromResource, File clonedPath) {
		this.builtBy = builtBy;
		this.pending = fromResource;
		this.clonedPath = clonedPath;
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
	
	@Override
	public File getPath() {
		if (actual == null)
			throw new QuickBuildException("Cannot use CloningResource before bound");
		return actual.getPath();
	}

	@Override
	public String compareAs() {
		if (actual == null)
			throw new QuickBuildException("Cannot use CloningResource before bound");
		return actual.compareAs();
	}

	@Override
	public Strategem getBuiltBy() {
		if (actual == null)
			return builtBy;
		return actual.getBuiltBy();
	}

	@Override
	public BuildResource cloneInto(CloningResource to) {
		throw new UtilException("Cannot clone a cloning resource");
	}
	
	@Override
	public int hashCode() {
		if (actual == null)
			return pending.hashCode();
		return compareAs().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof BuildResource))
			return false;
		if (actual == null)
			return pending.equals(obj);
		return compareAs().equals(((BuildResource)obj).compareAs());
	}

	@Override
	public String toString() {
		if (actual != null)
			return "Cloned["+actual+"]";
		return "CloneTo[" + clonedPath + "]";
	}
}
