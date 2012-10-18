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

	public void asJson(JSBuilder sb) {
		sb.open();
		for (Stmt s : stmts)
			s.toScript(sb);
		sb.close();
	}

	public boolean isEmpty() {
		return stmts.isEmpty();
	}

	public Var declare(String name, JSExpr expr) {
		Var var = new Var(name);
		add(new Assign(var, expr, true));
		return var;
	}

	public void assign(ArrayIndex a, JSExpr expr) {
		add(new Assign(a, expr));
	}

	public void assign(Member m, JSExpr expr) {
		add(new Assign(m, expr));
	}

	public void assign(Var v, ArrayIndex expr) {
		add(new Assign(v, expr, false));
	}

	public IfElseStmt ifelse() {
		IfElseStmt stmt = new IfElseStmt();
		add(stmt);
		return stmt;
	}

	public ForPropsStmt forProps(Var takes, Var over) {
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

	public ForEachStmt forEach(Var takes, JSExpr over) {
		ForEachStmt ret = new ForEachStmt(takes, over);
		add(ret);
		return ret;
	}
}
