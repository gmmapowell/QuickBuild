package com.gmmapowell.bytecode;

public class ArrayItem extends Expr {
	private final String ofType;
	private final Expr array;
	private final int idx;

	public ArrayItem(MethodCreator methodCreator, String ofType, Expr array, int idx) {
		super(methodCreator);
		this.ofType = ofType;
		this.array = array;
		this.idx = idx;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		array.spitOutByteCode(meth);
		meth.iconst(idx);
		meth.aaload();
	}

	@Override
	public String getType() {
		return ofType;
	}

}
