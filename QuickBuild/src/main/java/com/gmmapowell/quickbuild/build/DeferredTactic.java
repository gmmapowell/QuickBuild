package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.DependencyFloat;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.PrettyPrinter;

public class DeferredTactic extends BandElement {
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

	@Override
	public int size() {
		return 1;
	}

	@Override
	public Tactic tactic(int currentTactic) {
		if (currentTactic == 0)
			return getTactic();
		return null;
	}

	@Override
	public boolean isDeferred(Tactic tactic) {
		return false;
	}

	@Override
	public void print(PrettyPrinter pp, boolean withTactics) {
		pp.append("Deferred");
		pp.requireNewline();
		if (withTactics)
		{
			pp.indentMore();
			pp.append(id);
			pp.requireNewline();
			pp.indentLess();
		}
	}

	@Override
	public boolean isFirstTactic(Tactic tactic) {
		return tactic == this.tactic;
	}

	@Override
	public boolean isLastTactic(Tactic tactic) {
		return tactic == this.tactic;
	}

	@Override
	public Strategem getStrat() {
		return tactic.belongsTo();
	}

	@Override
	public Iterable<BuildResource> getDependencies(DependencyManager manager) {
		List<BuildResource> ret = new ArrayList<BuildResource>();
		for (BuildResource br : manager.getDependencies(getStrat()))
			ret.add(br);
		for (BuildResource br : ((DependencyFloat)tactic).needsAdditionalBuiltResources())
			ret.add(br);
		return ret;
	}

	@Override
	public boolean isNotApplicable() {
		return fromES.isNotApplicable();
	}
}