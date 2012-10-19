package com.gmmapowell.jsgen;

import com.gmmapowell.exceptions.UtilException;

public class IfElseStmt extends Stmt {
	public JSExpr test;
	public final JSBlock yes;
	public final JSBlock no;
	
	IfElseStmt(JSScope scope) {
		yes = new JSBlock(scope);
		no = new JSBlock(scope);
	}

	public void and(JSExpr left, JSExpr right) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = new BinaryOp("&&", left, right);
	}

	public void equality(JSExpr left, JSExpr right) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = new BinaryOp("==", left, right);
	}

	public void isTruthy(JSExpr nkey) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = nkey;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("if (");
		if (test == null)
			throw new UtilException("test cannot be null in an if statement");
		test.toScript(sb);
		sb.append(")");
		yes.toScript(sb);
		if (!no.isEmpty()) {
			sb.append(" else ");
			no.toScript(sb);
		}
	}
}