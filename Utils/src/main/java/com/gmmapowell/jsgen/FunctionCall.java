package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;

public class FunctionCall extends JSExpr {
	private final String fn;
	private final List<JSExpr> args = new ArrayList<JSExpr>();
	protected final JSScope scope;

	FunctionCall(JSScope scope, String fn) {
		this.scope = scope;
		this.fn = fn;
	}

	// If you already have the argument ...
	public void arg(JSExpr a) {
		args.add(a);
	}

	// If you want to create something just for here
	public JSExprGenerator arg() {
		JSExprGenerator ret = new JSExprGenerator(scope);
		args.add(ret);
		return ret;
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