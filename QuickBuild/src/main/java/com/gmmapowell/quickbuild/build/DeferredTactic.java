package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.PrettyPrinter;

public class DeferredTactic {
	private final String id;
	private Tactic tactic;
	private ExecuteStrategem fromES;

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

	public void bind(ExecuteStrategem from, Tactic tactic) {
		if (this.tactic != null)
			throw new RuntimeException("Cannot bind tactic twice");
		this.fromES = from;
		this.tactic = tactic;
	}

	public void print(PrettyPrinter pp) {
		pp.append(id);
		pp.requireNewline();
	}

	public boolean isBound() {
		return tactic != null;
	}
	
	@Override
	public String toString() {
		return "Deferred["+tactic+"]";
	}

	public boolean isClean() {
		return fromES.isClean();
	}
}