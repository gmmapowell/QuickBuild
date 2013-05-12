package com.gmmapowell.jsgen;

public class JSReturn extends Stmt {

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("return");
		sb.semi(true);
	}

}
