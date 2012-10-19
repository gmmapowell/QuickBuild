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

	public IfElseStmt and(JSExpr left, JSExpr right) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = new BinaryOp("&&", left, right);
		return this;
	}

	public IfElseStmt equality(JSExpr left, JSExpr right) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = new BinaryOp("==", left, right);
		return this;
	}

	public IfElseStmt isTruthy(JSExpr nkey) {
		if (test != null)
			throw new UtilException("Cannot specify more than one test for an if block");
		test = nkey;
		return this;
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