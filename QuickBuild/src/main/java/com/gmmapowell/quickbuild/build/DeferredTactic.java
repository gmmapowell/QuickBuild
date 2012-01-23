package com.gmmapowell.quickbuild.build;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.DependencyFloat;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.CantRunDeferredTacticYetException;
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
		// For what it's worth, I think probably the easiest thing to do in this situation is to throw a (typed) exception here which
		// can be caught somewhere where someone can do something (I'm seeing this in BuildExecutor/next calling BuildOrder/get)
		// and they can then:
		//  first check that the thing this is dependent on is back in the well and not built
		//  secondly blow away both ends of this DeferredTactic (the deferring and the executing)
		//    - if it needs splitting, that will happen
		if (tactic == null)
			throw new CantRunDeferredTacticYetException("Deferred tactic " + id + " was never bound.  Having analyzed this, I have decided the problem is that this is not dependent (enough) on the thing it was deferred from.  So if that is rejected at some point, then this may need to be rejected (and possibly re-assimilated) at some point");
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

	@Override
	public void fail() {
		fromES.fail();
	}
}