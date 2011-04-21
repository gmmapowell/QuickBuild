package com.gmmapowell.quickbuild.core;

import java.io.File;

import com.gmmapowell.utils.FileUtils;

public abstract class SolidResource implements BuildResource {

	protected final File file;
	protected final File relative;
	protected final Strategem parent;

	protected SolidResource(Strategem builtBy, File path)
	{
		parent = builtBy;
		file = path;
		this.relative = FileUtils.makeRelative(path); 
	}
	
	public abstract String compareAs();

	@Override
	public Strategem getBuiltBy() {
		return parent;
	}

	@Override
	public File getPath() {
		return file;
	}

	@Override
	public int hashCode() {
		return compareAs().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof SolidResource))
			return false;
		return compareAs().equals(((SolidResource)obj).compareAs());
	}
	
	public String toString()
	{
		return compareAs();
	}
}
