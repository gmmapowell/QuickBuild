package com.gmmapowell.quickbuild.core;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public abstract class SolidResource implements BuildResource, Comparable<BuildResource> {

	protected final File file;
	protected final File relative;
	protected final Tactic parent;
	private boolean analyze;

	protected SolidResource(Tactic builtBy, File path)
	{
		parent = builtBy;
		file = path;
		File tmp = null;
		try
		{
			tmp = FileUtils.makeRelative(path);
		}
		catch (Exception ex)
		{ /* no worries */ }
		this.relative =  tmp;
	}
	
	public abstract String compareAs();
	
	@Override
	public Tactic getBuiltBy() {
		return parent;
	}

	@Override
	public File getPath() {
		return file;
	}

	@Override
	public BuildResource cloneInto(CloningResource to) {
		throw new UtilException("Cannot clone into " + this + " - implement it in the appropriate subclass " + this.getClass());
	}
	
	/*
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
	*/
	@Override
	public void enableAnalysis() {
		analyze = true;
	}

	@Override
	public boolean doAnalysis() {
		return analyze;
	}

	@Override
	public int compareTo(BuildResource o) {
		return toString().compareTo(o.toString());
	}

	public String toString()
	{
		return compareAs();
	}
}
