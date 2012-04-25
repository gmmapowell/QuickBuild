package com.gmmapowell.bytecode;

public abstract class Expr {
	protected final MethodDefiner meth;

	public Expr(NewMethodDefiner meth) {
		this.meth = (MethodDefiner) meth;
	}

	public void flush()
	{
		spitOutByteCode(meth);
	}
	
	public abstract void spitOutByteCode(MethodDefiner meth);

	public abstract String getType();
}
