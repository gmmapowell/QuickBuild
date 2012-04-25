package com.gmmapowell.bytecode;

public class ClassConstExpr extends Expr {

	private final String cls;

	public ClassConstExpr(MethodDefiner meth, String cls) {
		super(meth);
		this.cls = cls;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		meth.ldcClass(cls);
	}

	@Override
	public String getType() {
		return "java.lang.Class";
	}

}
