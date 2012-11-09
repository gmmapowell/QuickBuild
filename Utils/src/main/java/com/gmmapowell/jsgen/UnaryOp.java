package com.gmmapowell.jsgen;

public class UnaryOp extends JSExpr {

	private final String op;
	private final JSExpr expr;

	public UnaryOp(String op, JSExpr expr) {
		this.op = op;
		this.expr = expr;
	}

	@Override
	public void toScript(JSBuilder sb) {
		// TODO: We probably need to consider precedence at some point
		sb.append(" " + op + " ");
		expr.toScript(sb);
	}

}