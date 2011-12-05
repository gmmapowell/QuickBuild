package com.gmmapowell.bytecode;

public class ClassConstExpr extends Expr {

	private final String cls;

	public ClassConstExpr(MethodCreator meth, String cls) {
		super(meth);
		this.cls = cls;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		meth.ldcClass(cls);
	}

	@Override
	public String getType() {
		return "java.lang.Class";
	}

}
