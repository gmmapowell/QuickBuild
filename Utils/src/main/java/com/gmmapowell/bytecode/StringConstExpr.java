package com.gmmapowell.bytecode;

public class StringConstExpr extends Expr {

	private final String str;

	public StringConstExpr(MethodDefiner meth, String str) {
		super(meth);
		this.str = str;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		meth.ldcString(str);
	}

	@Override
	public String getType() {
		return "java.lang.String";
	}

}
