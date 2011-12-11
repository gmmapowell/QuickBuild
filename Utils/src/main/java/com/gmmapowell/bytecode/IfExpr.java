package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class IfExpr extends Expr {
	private final Expr test;
	private final Expr then;
	private final Expr orelse;

	public IfExpr(MethodCreator meth, Expr left, Expr right, Expr then, Expr orelse) {
		super(meth);
		test = new EqualsExpr(meth, left, right);
		this.then = then;
		this.orelse = orelse;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		test.spitOutByteCode(meth);
		Marker m1 = meth.ifeq();
		then.spitOutByteCode(meth);
		Marker m2 = meth.jump();
		m1.setHere();
		orelse.spitOutByteCode(meth);
		m2.setHere();
	}

	@Override
	public String getType() {
		throw new UtilException("This is void, which might, in some cases, be valid, but I haven't seen one yet");
	}

}
