package com.gmmapowell.jsgen;


public class ForPropsStmt extends AbstractForStmt {
	private final Var over;

	public ForPropsStmt(Var takes, Var over) {
		super(takes);
		this.over = over;
	}

	@Override
	public void constructFor(JSBuilder sb) {
		sb.append("for (var " + takes + " in "+over+")");
	}

}