package com.gmmapowell.bytecode;

public class BoolConstExpr extends Expr {

	private final int value;

	public BoolConstExpr(MethodDefiner meth, boolean b) {
		super(meth);
		this.value = b?1:0;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		meth.iconst(value);
	}

	@Override
	public String getType() {
		return "boolean";
	}

}
