package com.gmmapowell.bytecode;

public class EqualsExpr extends Expr {
	private final Expr left;
	private final Expr right;

	public EqualsExpr(MethodDefiner meth, Expr left, Expr right) {
		super(meth);
		this.left = left;
		this.right = right;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		left.spitOutByteCode(meth);
		right.spitOutByteCode(meth);
		meth.invokeVirtualMethod(left.getType(), "boolean", "equals", "java.lang.Object");
	}

	@Override
	public String getType() {
		return "boolean";
	}

}
