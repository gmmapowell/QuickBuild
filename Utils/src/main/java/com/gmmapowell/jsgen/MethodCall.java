package com.gmmapowell.jsgen;

public class MethodCall extends FunctionCall {
	private final JSExpr expr;

	public MethodCall(JSExpr expr, String fn, JSExpr... args) {
		super(fn, args);
		this.expr = expr;
	}

	@Override
	public void toScript(JSBuilder sb) {
		expr.toScript(sb);
		sb.append(".");
		super.toScript(sb);
	}
}