package com.gmmapowell.jsgen;


public class ForPropsStmt extends AbstractForStmt {
	private final JSVar over;

	public ForPropsStmt(JSVar takes, JSVar over) {
		super(takes);
		this.over = over;
	}

	@Override
	public void constructFor(JSBuilder sb) {
		sb.append("for (var " + takes + " in "+over+")");
	}

}