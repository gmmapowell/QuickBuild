package com.gmmapowell.jsgen;

public class JSObjectExpr extends JSExpr {

	@Override
	public void toScript(JSBuilder sb) {
		sb.ocb();
		// TODO: members
		sb.ccb();
	}

}