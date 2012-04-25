package com.gmmapowell.bytecode;

public class NullExpr extends Expr {

	public NullExpr(MethodDefiner meth) {
		super(meth);
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		meth.aconst_null();
	}

	@Override
	public String getType() {
		return "java.lang.Object";
	}
}
