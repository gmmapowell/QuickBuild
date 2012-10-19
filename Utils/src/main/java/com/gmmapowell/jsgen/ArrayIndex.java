package com.gmmapowell.jsgen;

public class ArrayIndex extends JSExpr {
	private final JSExpr array;
	private final JSExpr idx;

	public ArrayIndex(JSExpr array, JSExpr idx) {
		this.array = array;
		this.idx = idx;
	}

	@Override
	public void toScript(JSBuilder sb) {
		array.toScript(sb);
		sb.osb();
		idx.toScript(sb);
		sb.csb();
	}

}