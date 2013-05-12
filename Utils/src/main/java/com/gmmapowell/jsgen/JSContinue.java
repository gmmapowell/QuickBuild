package com.gmmapowell.jsgen;

public class JSContinue extends Stmt {

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("continue");
		sb.semi(true);
	}

}
