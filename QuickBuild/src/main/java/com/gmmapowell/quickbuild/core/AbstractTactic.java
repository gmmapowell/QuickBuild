package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.OrderedFileList;

public abstract class AbstractTactic implements Tactic, Comparable<Tactic> {
	protected final Strategem parent;
	private final Set <Tactic> procDeps = new HashSet<Tactic>();
	private final ResourcePacket<PendingResource> needsResources = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> providesResources = new ResourcePacket<BuildResource>();
	private final ResourcePacket<BuildResource> buildsResources = new ResourcePacket<BuildResource>();

	public AbstractTactic(Strategem parent) {
		this.parent = parent;
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	public void needs(PendingResource pr) {
		needsResources.add(pr);
	}
	
	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needsResources;
	}
	
	public void provides(BuildResource br) {
		providesResources.add(br);
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return providesResources;
	}

	public void builds(BuildResource br) {
		buildsResources.add(br);
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return buildsResources;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return null;
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}

	@Override
	public void addProcessDependency(Tactic earlier) {
		if (earlier == null)
			throw new UtilException("Cannot add null dependency");
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}

	@Override
	public int compareTo(Tactic o) {
		return identifier().compareTo(o.identifier());
	}
}
