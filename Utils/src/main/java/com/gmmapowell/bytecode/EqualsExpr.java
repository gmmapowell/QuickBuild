package com.gmmapowell.bytecode;

public class EqualsExpr extends Expr {
	private final Expr left;
	private final Expr right;

	public EqualsExpr(MethodCreator meth, Expr left, Expr right) {
		super(meth);
		this.left = left;
		this.right = right;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		left.spitOutByteCode(meth);
		right.spitOutByteCode(meth);
		meth.invokeVirtualMethod("java.lang.Object", "boolean", "equals", "java.lang.Object");
	}

	@Override
	public String getType() {
		return "boolean";
	}

}
