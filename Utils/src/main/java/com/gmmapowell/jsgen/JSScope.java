package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;

public class JSScope {
	private final JSScope parent;
	private final List<JSVar> vars = new ArrayList<JSVar>();

	public JSScope(JSScope parent) {
		this.parent = parent;
	}

	public LValue resolveClass(String name) {
		JSVar ret = new JSVar(this, name, true);
		vars.add(ret);
		return ret;
	}

	public JSVar getVarLike(String s) {
		JSVar ret = new JSVar(this, s, false);
		vars.add(ret);
		return ret;
	}

	public JSVar getExactVar(String s) {
		JSVar ret = new JSVar(this, s, true);
		vars.add(ret);
		return ret;
	}

	public List<JSVar> allScopedVars() {
		List<JSVar> ret = new ArrayList<JSVar>();
		applyVars(ret);
		return ret;
	}

	private void applyVars(List<JSVar> ret) {
		if (parent != null)
			parent.applyVars(ret);
		ret.addAll(vars);
	}
}
