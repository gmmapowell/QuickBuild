package com.gmmapowell.quickbuild.build;

import com.gmmapowell.quickbuild.core.Tactic;

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

}
