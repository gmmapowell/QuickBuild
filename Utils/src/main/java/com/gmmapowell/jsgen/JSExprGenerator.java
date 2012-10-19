package com.gmmapowell.jsgen;

import com.gmmapowell.exceptions.UtilException;

public class JSExprGenerator extends JSExpr {
	private final JSScope scope;
	private JSExpr expr;

	JSExprGenerator(JSScope scope) {
		this.scope = scope;
	}
	
	@Override
	public void toScript(JSBuilder sb) {
		if (expr == null)
			throw new UtilException("Cannot handle placeholder which was never used");
		expr.toScript(sb);
	}

	public void value(String string) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		expr = new JSValue(string);
	}
	
	public MethodCall methodCall(JSExpr obj, String method) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		expr = new MethodCall(scope, obj, method);
		return (MethodCall) expr;
	}

	public MethodCall methodCall(JSMethodInvoker method) {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		expr = new MethodCall(scope, method);
		return (MethodCall) expr;
	}

	public JSObjectExpr literalObject() {
		if (expr != null)
			throw new UtilException("Can only specify one value in a placeholder");
		JSObjectExpr ret = new JSObjectExpr(scope);
		expr = ret;
		return ret;
	}
}