package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.PrettyPrinter;

public class DeferredTactic {
	private final String id;
	private Tactic tactic;

	public DeferredTactic(String id) {
		this.id = id;
	}

	public boolean is(String name) {
		return id.equals(name);
	}

	public String name() {
		return id;
	}

	public Tactic getTactic() {
		if (tactic == null)
			throw new RuntimeException("Something went wrong in the wiring up of deferred tactic " + this);
		return tactic;
	}

	public void bind(Tactic tactic) {
		if (this.tactic != null)
			throw new RuntimeException("Cannot bind tactic twice");
		this.tactic = tactic;
	}

	public void print(PrettyPrinter pp) {
		pp.append(id);
		pp.requireNewline();
	}
}