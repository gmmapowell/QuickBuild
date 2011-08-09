package com.gmmapowell.quickbuild.build.ftp;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class DistributeResource implements BuildResource {
	private final DistributeCommand strat;
	private final String host;

	public DistributeResource(DistributeCommand strat, String host) {
		this.strat = strat;
		this.host = host;
	}

	@Override
	public Strategem getBuiltBy() {
		return strat;
	}

	@Override
	public File getPath() {
		return null;
	}

	@Override
	public String compareAs() {
		return "DistributeTo["+host+"]";
	}

	@Override
	public BuildResource cloneInto(CloningResource toResource) {
		throw new RuntimeException("I don't really understand this");
	}
	
	@Override
	public String toString() {
		return compareAs();
	}
}
