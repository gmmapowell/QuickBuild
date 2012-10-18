package com.gmmapowell.jsgen;

public class BinaryOp extends JSExpr {

	private final String op;
	private final JSExpr left;
	private final JSExpr right;

	public BinaryOp(String op, JSExpr left, JSExpr right) {
		this.op = op;
		this.left = left;
		this.right = right;
	}

	@Override
	public void toScript(JSBuilder sb) {
		// TODO: We probably need to consider precedence at some point
		left.toScript(sb);
		sb.append(" " + op + " ");
		right.toScript(sb);
	}

}