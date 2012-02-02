package com.gmmapowell.bytecode;

public class BoolConstExpr extends Expr {

	private final int value;

	public BoolConstExpr(MethodCreator meth, boolean b) {
		super(meth);
		this.value = b?1:0;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		meth.iconst(value);
	}

	@Override
	public String getType() {
		return "boolean";
	}

}
