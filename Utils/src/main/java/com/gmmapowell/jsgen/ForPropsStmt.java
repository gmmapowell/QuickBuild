package com.gmmapowell.jsgen;


public class ForPropsStmt extends AbstractForStmt {
	private final JSVar over;

	public ForPropsStmt(JSScope scope, String takes, JSVar over) {
		super(scope, takes);
		this.over = over;
	}

	@Override
	public void constructFor(JSBuilder sb) {
		sb.append("for (var " + takes + " in "+over+")");
	}

}