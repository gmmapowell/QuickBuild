package com.gmmapowell.bytecode;

public class UseAsType extends Expr {
	private final Expr expr;
	private final String newType;

	public UseAsType(MethodDefiner meth, Expr expr, String newType) {
		super(meth);
		this.expr = expr;
		this.newType = newType;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		expr.spitOutByteCode(meth);
	}

	@Override
	public String getType() {
		return newType;
	}

}
