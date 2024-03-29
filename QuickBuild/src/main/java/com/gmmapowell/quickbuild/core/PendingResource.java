package com.gmmapowell.quickbuild.core;

import java.io.File;
import java.util.List;

import org.zinutils.exceptions.UtilException;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class PendingResource implements BuildResource {
	private String pendingName;
	private BuildResource boundTo;

	public PendingResource(String from) {
		this.pendingName = from;
	}
	
	public PendingResource(BuildResource real) {
		this.boundTo = real;
	}

	@Override
	public File getPath() {
		if (boundTo == null)
			throw new QuickBuildException("Cannot use PendingResource " + pendingName + " before bound");
		return boundTo.getPath();
	}
	
	@Override
	public List<File> getPaths() {
		return boundTo.getPaths();
	}

	public String getPending() {
		return pendingName;
	}
	
	@Override
	public String compareAs() {
		if (boundTo != null)
			return boundTo.compareAs();
		return pendingName;
	}

	@Override
	public Tactic getBuiltBy() {
		if (boundTo == null)
			throw new QuickBuildException("Cannot use PendingResource before bound");
		return boundTo.getBuiltBy();
	}

	public BuildResource physicalResource() {
		if (boundTo == null)
			throw new QuickBuildException("Cannot use PendingResource (" + pendingName + ") before bound");
		return boundTo;
	}

	public void bindTo(BuildResource uniq) {
		if (boundTo != null)
			throw new QuickBuildException("Cannot bind PendingResource multiple times");
		if (uniq instanceof PendingResource)
			throw new QuickBuildException("Invalid bind type " + uniq + " of " + uniq.getClass());
		boundTo = uniq;
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
	
	@Override
	public void enableAnalysis() {
		throw new UtilException("Think Again");
	}

	@Override
	public boolean doAnalysis() {
		throw new UtilException("Think Again");
	}
		
	@Override
	public int compareTo(BuildResource o) {
		return toString().compareTo(o.toString());
	}

	@Override
	public String toString() {
		if (boundTo != null)
			return "Pended["+boundTo+"]";
		return "PendingResource["+pendingName+"]";
	}

	public boolean isBound() {
		return boundTo != null;
	}
}
