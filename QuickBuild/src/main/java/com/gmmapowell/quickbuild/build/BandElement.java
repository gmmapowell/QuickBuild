package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.PrettyPrinter;

public abstract class BandElement {
	private ExecutionBand band;
	protected List<DeferredTactic> deferred = new ArrayList<DeferredTactic>();

	public void add(DeferredTactic dt) {
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

	public abstract void print(PrettyPrinter pp);

	public abstract boolean isClean();

	public abstract boolean isLastTactic(Tactic tactic);
}
