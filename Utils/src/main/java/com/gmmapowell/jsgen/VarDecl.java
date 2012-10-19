package com.gmmapowell.jsgen;

import com.gmmapowell.exceptions.UtilException;

public class VarDecl extends JSExpr {
	private JSExpr expr;

	// make protected
	VarDecl() {
	}
	
	@Override
	public void toScript(JSBuilder sb) {
		expr.toScript(sb);
	}

	public MethodCall method(JSMethodInvoker method) {
		if (expr != null)
			throw new UtilException("Can only specify one value for a vardecl");
		expr = new MethodCall(method);
		return (MethodCall) expr;
	}

}
