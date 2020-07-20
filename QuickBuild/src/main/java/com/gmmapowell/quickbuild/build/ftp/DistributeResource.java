package com.gmmapowell.quickbuild.build.ftp;

import java.io.File;
import java.util.List;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Tactic;

public class DistributeResource implements BuildResource {
	private final DistributeCommand strat;
	private final String dir;
	private final String host;
	private boolean analyze;

	public DistributeResource(DistributeCommand strat, String dir, String host) {
		this.strat = strat;
		this.dir = dir;
		this.host = host;
	}

	@Override
	public Tactic getBuiltBy() {
		return strat;
	}

	@Override
	public File getPath() {
		return null;
	}

	@Override
	public List<File> getPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String compareAs() {
		return "DistributeTo-" + dir + "["+host+"]";
	}

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

	@Override
	public String toString() {
		return compareAs();
	}
}
