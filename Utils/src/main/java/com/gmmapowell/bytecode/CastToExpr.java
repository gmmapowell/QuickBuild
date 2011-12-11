package com.gmmapowell.bytecode;

public class CastToExpr extends Expr {
	private final Expr expr;
	private final String newType;

	public CastToExpr(MethodCreator meth, Expr expr, String newType) {
		super(meth);
		this.expr = expr;
		this.newType = newType;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		expr.spitOutByteCode(meth);
		meth.checkCast(newType);
	}

	@Override
	public String getType() {
		return newType;
	}

}
