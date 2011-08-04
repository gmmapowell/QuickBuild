package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.PrettyPrinter;

@Deprecated
public class Catchup extends BandElement {

	@Override
	public int size() {
		return deferred.size();
	}

	@Override
	public Tactic tactic(int currentTactic) {
		return deferred.get(currentTactic).getTactic();
	}

	@Override
	public boolean isDeferred(Tactic tactic) {
		return false;
	}

	@Override
	public void print(PrettyPrinter pp, boolean withTactics) {
		pp.append("Catchup");
		pp.requireNewline();
//		if (withTactics)
//		{
//			pp.indentMore();
//			for (DeferredTactic dt : deferred)
//				dt.print(pp);
//			pp.indentLess();
//		}
	}

	@Override
	public boolean isClean() {
		/* TODO: need reflective pointer back from strat to executeStrat
		for (DeferredTactic dt : deferred)
			if (dt.getTactic().belongsTo())
			*/
		return false;
	}

	@Override
	public boolean isLastTactic(Tactic tactic) {
		return deferred.get(deferred.size()-1).getTactic() == tactic;
	}
	
	@Override
	public String toString() {
		return "Catchup[" + deferred + "]";
	}

	@Override
	public Strategem getStrat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<BuildResource> getDependencies(DependencyManager manager) {
		// TODO Auto-generated method stub
		return null;
	}
}
