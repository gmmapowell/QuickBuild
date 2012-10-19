package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;

public class JSScope {
	private final JSScope parent;
	private final List<JSVar> vars = new ArrayList<JSVar>();

	public JSScope(JSScope parent) {
		this.parent = parent;
	}

	public JSVar getVarLike(String s) {
		return new JSVar(this, s, false);
	}

	public JSVar getExactVar(String s) {
		return new JSVar(this, s, true);
	}

}
