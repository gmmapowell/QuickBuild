package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.utils.OrderedFileList;

public abstract class AbstractTactic implements Tactic {
	protected final Strategem parent;
	private final Set <Tactic> procDeps = new HashSet<Tactic>();
	
	public AbstractTactic(Strategem parent) {
		this.parent = parent;
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
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
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}
}
