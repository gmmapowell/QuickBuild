package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Set;

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
	public void addProcessDependency(Tactic earlier) {
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}
}
