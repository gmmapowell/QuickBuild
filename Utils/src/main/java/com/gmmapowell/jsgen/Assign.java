package com.gmmapowell.jsgen;


public class Assign extends Stmt {
	private final JSExpr to;
	private final JSExpr expr;
	private final boolean declare;

	public Assign(JSVar to, JSExpr expr, boolean declare) {
		this.to = to;
		this.expr = expr;
		this.declare = declare;
	}

	public Assign(ArrayIndex arrayIndex, JSExpr expr) {
		to = arrayIndex;
		this.expr = expr;
		this.declare = false;
	}

	public Assign(LValue member, JSExpr expr) {
		to = member;
		this.expr = expr;
		this.declare = false;
	}

	@Override
	public void toScript(JSBuilder sb) {
		if (declare)
			sb.append("var ");
		to.toScript(sb);
		sb.assign();
		expr.toScript(sb);
		sb.semi();
	}

}