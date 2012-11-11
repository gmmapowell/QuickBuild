package com.gmmapowell.quickbuild.core;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;

public class ProcessResource implements BuildResource {
	private final Tactic tactic;

	public ProcessResource(Tactic t) {
		this.tactic = t;
	}

	public Tactic getTactic() {
		return tactic;
	}

	@Override
	public Tactic getBuiltBy() {
		// Is this a dumb question, or is it me?
		return tactic;
	}

	@Override
	public File getPath() {
		throw new UtilException("No");
	}

	@Override
	public String compareAs() {
		return "Process["+tactic.identifier()+"]";
	}

	@Override
	public BuildResource cloneInto(CloningResource toResource) {
		throw new UtilException("No");
	}

	@Override
	public void enableAnalysis() {
		throw new UtilException("No");
	}

	@Override
	public boolean doAnalysis() {
		throw new UtilException("No");
	}
	
	@Override
	public String toString() {
		return compareAs();
	}

	@Override
	public int hashCode() {
		return compareAs().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof BuildResource))
			return false;
		return compareAs().equals(((BuildResource)obj).compareAs());
	}
}
