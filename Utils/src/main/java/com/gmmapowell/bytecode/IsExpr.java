package com.gmmapowell.bytecode;

import com.gmmapowell.bytecode.IfExpr.IfCond;

public class IsExpr extends Expr {
	private IfExpr use;

	public IsExpr(MethodCreator meth, Expr test, Expr yes, Expr no, IfCond cond) {
		super(meth);
		use = new IfExpr(meth, test, yes, no, cond);
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		use.spitOutByteCode(meth);
	}

	@Override
	public String getType() {
		return "boolean";
	}

}
