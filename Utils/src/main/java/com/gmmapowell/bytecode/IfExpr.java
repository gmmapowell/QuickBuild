package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class IfExpr extends Expr {
	public enum IfCond {
		TRUE, FALSE, NULL, NOTNULL

	}

	private final Expr test;
	private final Expr then;
	private final Expr orelse;
	private final IfCond cond;

	public IfExpr(MethodCreator meth, Expr left, Expr right, Expr then, Expr orelse) {
		this(meth, new EqualsExpr(meth, left, right), then, orelse, IfCond.TRUE);
	}

	public IfExpr(MethodCreator meth, Expr test, Expr then, Expr orelse) {
		this(meth, test, then, orelse, IfCond.TRUE);
	}
	
	public IfExpr(MethodCreator meth, Expr test, Expr then, Expr orelse, IfCond cond) {
		super(meth);
		this.test = test;
		this.then = then;
		this.orelse = orelse;
		this.cond = cond;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		test.spitOutByteCode(meth);
		Marker m1;
		switch (cond)
		{
		case TRUE:
			m1 = meth.ifeq();
			break;
		case FALSE:
			m1 = meth.ifne();
			break;
		case NULL:
			m1 = meth.ifnull();
			break;
		case NOTNULL:
			m1 = meth.ifnonnull();
			break;
		default:
			throw new UtilException("There is no case " + cond);
		}
		if (then != null)
			then.spitOutByteCode(meth);
		if (orelse != null)
		{
			Marker m2 = meth.jump();
			m1.setHere();
			orelse.spitOutByteCode(meth);
			m2.setHere();
		}
		else
			m1.setHere();
	}

	@Override
	public String getType() {
		throw new UtilException("This is void, which might, in some cases, be valid, but I haven't seen one yet");
	}

}
