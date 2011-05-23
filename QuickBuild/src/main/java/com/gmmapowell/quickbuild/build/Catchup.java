package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.PrettyPrinter;

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
	public void print(PrettyPrinter pp) {
		pp.append("Catchup");
		pp.indentMore();
		for (DeferredTactic dt : deferred)
			dt.print(pp);
	}

	@Override
	public boolean isClean() {
		/* TODO: need reflective pointer back from strat to executeStrat
		for (DeferredTactic dt : deferred)
			if (dt.getTactic().belongsTo())
			*/
		return false;
	}
}
