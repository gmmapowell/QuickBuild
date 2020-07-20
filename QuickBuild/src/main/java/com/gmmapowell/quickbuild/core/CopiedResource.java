package com.gmmapowell.quickbuild.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

public class CopiedResource implements BuildResource {
	private final File clonedPath;
	private final Tactic builtBy;
	private final PendingResource pending;

	public CopiedResource(Tactic builtBy, PendingResource fromResource, File clonedPath) {
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

	
	@Override
	public File getPath() {
		return clonedPath;
	}

	@Override
	public List<File> getPaths() {
		return Arrays.asList(clonedPath);
	}

	@Override
	public String compareAs() {
		return "Copied[" + FileUtils.posixPath(clonedPath) + "]";
	}

	@Override
	public Tactic getBuiltBy() {
		return builtBy;
	}

	@Override
	public void enableAnalysis() {
		throw new UtilException("Think Again");
	}

	@Override
	public boolean doAnalysis() {
		return false;
	}

	@Override
	public int compareTo(BuildResource o) {
		return compareAs().compareTo(o.compareAs());
	}

	@Override
	public String toString() {
		return "CopiedTo[" + FileUtils.posixPath(clonedPath) + "]";
	}
}
