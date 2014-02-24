package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.OrderedFileList;

public abstract class AbstractTactic implements Tactic, Comparable<Tactic> {
	protected final Strategem parent;
	private final Set <Tactic> procDeps = new HashSet<Tactic>();
	private ResourcePacket<PendingResource> needsResources;

	public AbstractTactic(Strategem parent) {
		this.parent = parent;
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	public void needs(PendingResource pr) {
		if (needsResources == null)
			needsResources = new ResourcePacket<PendingResource>();
		needsResources.add(pr);
	}
	
	@Override
	public ResourcePacket<PendingResource> needsResources() {
		if (needsResources != null)
			return needsResources;
		return parent.needsResources();
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return parent.providesResources();
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return parent.buildsResources();
	}

	@Override
	public OrderedFileList sourceFiles() {
		return parent.sourceFiles();
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
