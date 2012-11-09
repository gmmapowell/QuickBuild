package com.gmmapowell.jsgen;

public class JSThis extends LValue {

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("this");
	}

}
