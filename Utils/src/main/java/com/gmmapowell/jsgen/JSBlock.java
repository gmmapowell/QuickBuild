package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;


public class JSBlock {
	private final List<Stmt> stmts = new ArrayList<Stmt>();

	public void add(Stmt stmt) {
		stmts.add(stmt);
	}

	public void add(JSExpr voidExpr) {
		stmts.add(new VoidExprStmt(voidExpr));
	}

	public void toScript(JSBuilder sb) {
		sb.ocb();
		for (Stmt s : stmts)
			s.toScript(sb);
		sb.ccb();
	}

	public boolean isEmpty() {
		return stmts.isEmpty();
	}

	public JSVar declare(String name, JSExpr expr) {
		JSVar var = new JSVar(name);
		add(new Assign(var, expr, true));
		return var;
	}

	public void assign(ArrayIndex a, JSExpr expr) {
		add(new Assign(a, expr));
	}

	public void assign(Member m, JSExpr expr) {
		add(new Assign(m, expr));
	}

	public void assign(JSVar v, ArrayIndex expr) {
		add(new Assign(v, expr, false));
	}

	public IfElseStmt ifelse() {
		IfElseStmt stmt = new IfElseStmt();
		add(stmt);
		return stmt;
	}

	public ForPropsStmt forProps(JSVar takes, JSVar over) {
		ForPropsStmt ret = new ForPropsStmt(takes, over);
		add(ret);
		return ret;
	}

	public void voidCall(String name, JSExpr... args) {
		add(new FunctionCall(name, args));
	}

	public void voidStmt(JSExpr expr) {
		add(new VoidExprStmt(expr));
	}

	public ForEachStmt forEach(JSVar takes, JSExpr over) {
		ForEachStmt ret = new ForEachStmt(takes, over);
		add(ret);
		return ret;
	}
}
