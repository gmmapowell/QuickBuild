package com.gmmapowell.jsgen;

public class FunctionCall extends JSExpr {
	private final String fn;
	private final JSExpr[] args;

	public FunctionCall(String fn, JSExpr... args) {
		this.fn = fn;
		this.args = args;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append(fn);
		sb.append("(");
		String sep = "";
		for (JSExpr ce : args)
		{
			sb.append(sep);
			ce.toScript(sb);
			sep = ",";
		}
		sb.append(")");
	}

}