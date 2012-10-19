package com.gmmapowell.jsgen;

public class JSMember extends LValue {
	private final LValue inside;
	private final String member;

	JSMember(LValue inside, String member) {
		this.inside = inside;
		this.member = member;
	}
	
	JSExpr getOwner() {
		return inside;
	}
	
	String getChild() {
		return member;
	}

	public JSMethodInvoker method(String method) {
		return new JSMethodInvoker(new JSMember(this, method));
	}

	@Override
	public void toScript(JSBuilder sb) {
		inside.toScript(sb);
		sb.append(".");
		sb.append(member);
	}
}