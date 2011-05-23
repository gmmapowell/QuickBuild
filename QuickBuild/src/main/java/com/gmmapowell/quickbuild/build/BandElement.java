package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.core.Tactic;

public abstract class BandElement {
	protected List<DeferredTactic> deferred = new ArrayList<DeferredTactic>();

	public void add(DeferredTactic dt) {
		deferred.add(dt);
	}

	public Iterable<DeferredTactic> deferred() {
		return deferred;
	}

	public abstract int size();

	public abstract Tactic tactic(int currentTactic);

	public abstract boolean isDeferred(Tactic tactic);
}
