package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Set;

import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.OrderedFileList;

public abstract class AbstractStrategemTactic extends AbstractStrategem implements Tactic, Comparable<Tactic> {
	private final Set <Tactic> procDeps = new HashSet<Tactic>();
	private final ResourcePacket<PendingResource> needsResources = new ResourcePacket<PendingResource>();;
	private final ResourcePacket<BuildResource> providesResources = new ResourcePacket<BuildResource>();
	private final ResourcePacket<BuildResource> buildsResources = new ResourcePacket<BuildResource>();

	public AbstractStrategemTactic(TokenizedLine toks, ArgumentDefinition... args) {
		super(toks, args);
		tactics.add(this);
	}

	public AbstractStrategemTactic(Class<? extends ConfigApplyCommand>[] clzs) {
		super(clzs);
		tactics.add(this);
	}

	public void needs(PendingResource pr) {
		needsResources.add(pr);
	}
	
	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needsResources;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return providesResources;
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return buildsResources;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return null;
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

	@Override
	public boolean analyzeExports() {
		return false;
	}
}
