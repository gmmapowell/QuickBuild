package com.gmmapowell.jsgen;

public class Member extends LValue {

	private final LValue inside;
	private final String member;

	public Member(LValue inside, String member) {
		this.inside = inside;
		this.member = member;
	}

	@Override
	public void toScript(JSBuilder sb) {
		inside.toScript(sb);
		sb.append(".");
		sb.append(member);
	}

}