package com.gmmapowell.bytecode;

public class InstanceOf extends Expr {

	private final String ofClz;
	private final Expr expr;

	public InstanceOf(MethodDefiner meth, Expr expr, String ofClz) {
		super(meth);
		this.expr = expr;
		this.ofClz = ofClz;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		expr.spitOutByteCode(meth);
		meth.isInstanceOf(ofClz);
	}

	@Override
	public String getType() {
		return "boolean";
	}

}
