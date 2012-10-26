package com.gmmapowell.jsgen;


public class ForPropsStmt extends AbstractForStmt {
	private final JSExpr over;

	public ForPropsStmt(JSScope scope, String takes, JSExpr fields) {
		super(scope, takes);
		this.over = fields;
	}

	@Override
	public void constructFor(JSBuilder sb) {
		sb.ident("for");
		sb.orb();
		sb.ident("var");
		getLoopVar().toScript(sb);
		sb.ident("in");
		over.toScript(sb);
		sb.crb();
	}

}