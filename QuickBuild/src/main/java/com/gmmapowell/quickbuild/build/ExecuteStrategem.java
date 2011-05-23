package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;


public class ExecuteStrategem extends BandElement {
	private final String which;
	private boolean clean = true;
	private Set<String> dependsOn = new HashSet<String>();
	private List<Tactic> tactics = new ArrayList<Tactic>();
	private ExecutionBand band;

	public ExecuteStrategem(String which) {
		this.which = which;
	}
	
	public ExecutionBand inBand() {
		return band;
	}
	
	public void bind(ExecutionBand band, Strategem strat)
	{
		this.band = band;
		if (this.tactics.size() == 0)
			this.tactics.addAll(strat.tactics());
	}
	
	public boolean isClean() {
		return clean;
	}

	public void markDirty() {
		clean = false;
	}

	public String name() {
		return which;
	}

	@Override
	public int size() {
		return tactics.size();
	}

	@Override
	public Tactic tactic(int currentTactic) {
		return tactics.get(currentTactic);
	}

	@Override
	public boolean isDeferred(Tactic tactic) {
		String id = tactic.identifier();
		for (DeferredTactic dt : deferred)
			if (dt.is(id))
			{
				dt.bind(tactic);
				return true;
			}
		return false;
	}
	
	@Override
	public String toString() {
		return which;
	}
}