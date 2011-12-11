package com.gmmapowell.bytecode;

public class NullExpr extends Expr {

	public NullExpr(MethodCreator meth) {
		super(meth);
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		meth.aconst_null();
	}

	@Override
	public String getType() {
		return "java.lang.Object";
	}
}
