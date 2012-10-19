package com.gmmapowell.jsgen;

public class JSVar extends LValue {
	private final String var;

	public JSVar(String var) {
		this.var = var;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append(var);
	}

	public String getName() {
		return var;
	}
}