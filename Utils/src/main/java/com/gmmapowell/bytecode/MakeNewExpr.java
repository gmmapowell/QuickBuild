package com.gmmapowell.bytecode;

public class MakeNewExpr extends Expr {

	private final String ofClz;
	private final Expr[] args;

	public MakeNewExpr(MethodDefiner meth, String ofClz, Expr... args) {
		super(meth);
		this.ofClz = ofClz;
		this.args = args;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		meth.newObject(ofClz);
		meth.dup();
		String[] argTypes = new String[args.length];
		for (int i=0;i<args.length;i++)
		{
			Expr e = args[i];
			argTypes[i] = e.getType();
			e.spitOutByteCode(meth);
		}
		meth.invokeOtherConstructor(ofClz, argTypes);
	}

	@Override
	public String getType() {
		return ofClz;
	}

}
