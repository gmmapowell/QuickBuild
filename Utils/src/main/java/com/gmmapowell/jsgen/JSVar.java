package com.gmmapowell.jsgen;

public class JSVar extends LValue {
	private final JSScope scope;
	private final String var;
	private final boolean exact;

	JSVar(JSScope scope, String var, boolean exact) {
		this.scope = scope;
		this.var = var;
		this.exact = exact;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append(var);
	}

	public String getName() {
		return var;
	}
}