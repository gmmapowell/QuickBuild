package com.gmmapowell.bytecode;

public class BlockExpr extends Expr {

	private final Expr[] exprs;

	public BlockExpr(MethodDefiner meth, Expr[] exprs) {
		super(meth);
		this.exprs = exprs;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		for (Expr e: exprs)
		{
			e.spitOutByteCode(meth);
		}
	}

	@Override
	public String getType() {
		return null;
	}

	public boolean endsWithTransfer() {
		if (exprs.length == 0)
			return false;
		return IfExpr.isTransfer(exprs[exprs.length-1]);
	}

}
