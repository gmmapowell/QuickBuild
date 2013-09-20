package com.gmmapowell.jsgen;

public class JSBreak extends Stmt {

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("break");
		sb.semi(true);
	}

}
