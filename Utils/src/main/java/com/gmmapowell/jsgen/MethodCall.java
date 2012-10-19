package com.gmmapowell.jsgen;

public class MethodCall extends FunctionCall {
	private final JSExpr expr;

	MethodCall(JSExpr expr, String fn) {
		super(fn);
		this.expr = expr;
	}

	MethodCall(JSMethodInvoker method) {
		super(method.getName());
		this.expr = method.getTarget();
	}

	@Override
	public void toScript(JSBuilder sb) {
		expr.toScript(sb);
		sb.append(".");
		super.toScript(sb);
	}
}