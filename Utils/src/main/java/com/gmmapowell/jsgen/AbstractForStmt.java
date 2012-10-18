package com.gmmapowell.jsgen;


public abstract class AbstractForStmt extends Stmt {
	protected final Var takes;
	private final JSBlock block;

	public AbstractForStmt(Var takes) {
		this.takes = takes;
		this.block = new JSBlock();
	}

	public JSBlock nestedBlock() {
		return block;
	}

	@Override
	public final void toScript(JSBuilder sb) {
		constructFor(sb);
		block.asJson(sb);
	}

	protected abstract void constructFor(JSBuilder sb);
}