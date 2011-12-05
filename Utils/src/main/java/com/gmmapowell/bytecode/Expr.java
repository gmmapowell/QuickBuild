package com.gmmapowell.bytecode;

public abstract class Expr {
	protected final MethodCreator meth;

	public Expr(MethodCreator meth) {
		this.meth = meth;
	}

	public void flush()
	{
		spitOutByteCode(meth);
	}
	
	public abstract void spitOutByteCode(MethodCreator meth);

	public abstract String getType();
}
