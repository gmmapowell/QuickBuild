package com.gmmapowell.jsgen;


public abstract class LValue extends JSExpr {
	public JSMember member(String field) {
		return new JSMember(this, field);
	}
}