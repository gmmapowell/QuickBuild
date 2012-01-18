package com.gmmapowell.bytecode;

public class IntConstExpr extends Expr {

	private final int value;

	public IntConstExpr(MethodCreator meth, int i) {
		super(meth);
		this.value = i;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		meth.iconst(value);
	}

	@Override
	public String getType() {
		return "int";
	}

}
