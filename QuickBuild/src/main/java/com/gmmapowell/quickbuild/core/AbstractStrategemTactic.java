package com.gmmapowell.quickbuild.core;

import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;

public abstract class AbstractStrategemTactic extends AbstractStrategem implements Tactic {
	private final Set <Tactic> procDeps = new HashSet<Tactic>();

	public AbstractStrategemTactic(TokenizedLine toks, ArgumentDefinition... args) {
		super(toks, args);
		tactics.add(this);
	}

	public AbstractStrategemTactic(Class<? extends ConfigApplyCommand>[] clzs) {
		super(clzs);
		tactics.add(this);
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
}
