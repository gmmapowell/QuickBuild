package com.gmmapowell.jsgen;


public abstract class LValue extends JSExpr {
	public Member member(String field) {
		return new Member(this, field);
	}
}