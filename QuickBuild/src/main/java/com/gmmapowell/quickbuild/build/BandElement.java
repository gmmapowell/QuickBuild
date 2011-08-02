package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.PrettyPrinter;

public abstract class BandElement {
	private ExecutionBand band;
	protected List<DeferredTactic> deferred = new ArrayList<DeferredTactic>();
	private Set<Strategem> dependsOn = new TreeSet<Strategem>(Strategem.Comparator);

	public void defer(DeferredTactic dt) {
		deferred.add(dt);
	}

	public Iterable<DeferredTactic> deferred() {
		return deferred;
	}
	
	public void bind(ExecutionBand band)
	{
		this.band = band;
	}
	
	public ExecutionBand inBand() {
		return band;
	}
	
	public abstract int size();

	public abstract Tactic tactic(int currentTactic);

	public abstract boolean isDeferred(Tactic tactic);

	public boolean is(String identifier) {
		return false;
	}

	public abstract void print(PrettyPrinter pp, boolean withTactics);

	public abstract boolean isClean();

	public abstract boolean isLastTactic(Tactic tactic);

	public boolean isCompletelyClean() {
		return isClean();
	}
	
	public void dependsOn(Strategem mustHaveBuilt) {
		if (mustHaveBuilt == null)
			return;
		dependsOn.add(mustHaveBuilt);
	}
	
	public boolean hasPrereq(Strategem strat)
	{
		if (strat == null)
			return false;
		return dependsOn.contains(strat);
	}

	public void showRequires(PrettyPrinter pp) {
		for (Strategem s : dependsOn)
		{
			pp.append(s.identifier());
			pp.requireNewline();
		}
	}

	public boolean wouldBuild(Strategem mustHaveBuilt) {
		for (int i=0;i<size();i++)
			if (tactic(i).belongsTo().equals(mustHaveBuilt))
				return true;
		return false;
	}
}
