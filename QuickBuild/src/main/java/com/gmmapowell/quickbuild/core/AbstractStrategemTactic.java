package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;

public abstract class AbstractStrategemTactic extends AbstractStrategem implements Tactic, Comparable<Tactic> {
	private final Set <Tactic> procDeps = new HashSet<Tactic>();
	private ResourcePacket<PendingResource> needsResources;

	public AbstractStrategemTactic(TokenizedLine toks, ArgumentDefinition... args) {
		super(toks, args);
		tactics.add(this);
	}

	public AbstractStrategemTactic(Class<? extends ConfigApplyCommand>[] clzs) {
		super(clzs);
		tactics.add(this);
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
		return super.needsResources();
	}

	public final Strategem belongsTo() {
		return this;
	}

	@Override
	public void addProcessDependency(Tactic earlier) {
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
