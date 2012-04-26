package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class VoidExpr extends Expr {

	private final Expr cls;

	public VoidExpr(MethodDefiner meth, Expr ignoredResult) {
		super(meth);
		this.cls = ignoredResult;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		cls.spitOutByteCode(meth);
			meth.pop(cls.getType());
	}

	@Override
	public String getType() {
		throw new UtilException("This is void, which might, in some cases, be valid, but I haven't seen one yet");
	}

}
