package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class ThrowExpr extends Expr {
	private MakeNewExpr ex;

	public ThrowExpr(MethodDefiner meth, String clz, Expr... exprs) {
		super(meth);
		ex = meth.makeNew(clz, exprs);
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		ex.spitOutByteCode(meth);
		meth.athrow();
	}

	@Override
	public String getType() {
		throw new UtilException("This is void, which might, in some cases, be valid, but I haven't seen one yet");
	}

}
