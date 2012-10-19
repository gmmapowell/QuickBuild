package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;

public class FunctionCall extends JSExpr {
	private final String fn;
	private final List<JSExpr> args = new ArrayList<JSExpr>();

	FunctionCall(String fn) {
		this.fn = fn;
	}

	public void arg(JSExpr a) {
		args.add(a);
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