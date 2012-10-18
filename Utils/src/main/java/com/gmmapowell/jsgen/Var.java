package com.gmmapowell.jsgen;

public class Var extends LValue {
	private final String var;

	public Var(String var) {
		this.var = var;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append(var);
	}
}