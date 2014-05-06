package com.gmmapowell.bytecode;

public class TryCatch extends Expr {
	private final Expr inBlock;
	private final Expr handlerBlock;
	private final String exType;

	public TryCatch(MethodCreator meth, Expr inBlock, String exType, Expr handlerBlock) {
		super(meth);
		this.inBlock = inBlock;
		this.exType = exType;
		this.handlerBlock = handlerBlock;
	}

	@Override
	public void spitOutByteCode(MethodDefiner md) {
		MethodCreator meth = (MethodCreator)md;
		Marker m = meth.marker();
		inBlock.flush();
		Marker q = meth.marker();
		Marker r = meth.jump();
		meth.opstack(1);
		Marker s = meth.marker();
		Var ex = md.avar(exType, "ex");
		ex.store();
		handlerBlock.flush();
		r.setHere();
		meth.addException(exType, m, q, s);
	}

	@Override
	public String getType() {
		return inBlock.getType();
	}

}
