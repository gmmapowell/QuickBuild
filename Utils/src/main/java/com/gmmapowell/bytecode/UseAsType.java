package com.gmmapowell.bytecode;

public class UseAsType extends Expr {
	private final Expr expr;
	private final String newType;

	public UseAsType(MethodCreator meth, Expr expr, String newType) {
		super(meth);
		this.expr = expr;
		this.newType = newType;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		expr.spitOutByteCode(meth);
	}

	@Override
	public String getType() {
		return newType;
	}

}
