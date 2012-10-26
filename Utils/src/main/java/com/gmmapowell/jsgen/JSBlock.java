package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;


public class JSBlock {
	private final List<Stmt> stmts = new ArrayList<Stmt>();
	private final JSScope scope;
	private final boolean useParens;

	JSBlock(JSScope scope) {
		this(scope, true);
	}

	public JSBlock(JSScope scope, boolean useParens) {
		this.scope = scope;
		this.useParens = useParens;
	}

	public LValue resolveClass(String name) {
		return scope.resolveClass(name);
	}

	public void add(Stmt stmt) {
		stmts.add(stmt);
	}

	public void add(JSExpr voidExpr) {
		stmts.add(new VoidExprStmt(voidExpr));
	}

	public void toScript(JSBuilder sb) {
		if (useParens)
			sb.ocb();
		for (Stmt s : stmts)
			s.toScript(sb);
		if (useParens)
			sb.ccb();
	}

	public boolean isEmpty() {
		return stmts.isEmpty();
	}

	public JSVar declareVarLike(String var, Object expr) {
		JSVar ret = scope.getVarLike(var);
		JSExpr value;
		if (expr instanceof JSExpr)
			value = (JSExpr)expr;
		else if (expr instanceof String || expr instanceof Integer)
			value = new JSValue(expr);
		else
			throw new UtilException("Cannot handle value of type " + expr.getClass());
		add(new Assign(ret, value, true));
		return ret;
	}

	public VarDecl declareVarLike(String var) {
		JSVar jsvar = scope.getVarLike(var);
		VarDecl ret = new VarDecl(scope, jsvar);
		add(new Assign(jsvar, ret, true));
		return ret;
	}

	public void assign(ArrayIndex a, JSExpr expr) {
		add(new Assign(a, expr));
	}

	public void assign(LValue m, JSExpr expr) {
		add(new Assign(m, expr));
	}

	public void assign(JSVar v, ArrayIndex expr) {
		add(new Assign(v, expr, false));
	}

	public JSExprGenerator assign(LValue v) {
		JSExprGenerator ret = new JSExprGenerator(scope);
		add(new Assign(v, ret));
		return ret;
	}

	public IfElseStmt ifelse() {
		IfElseStmt stmt = new IfElseStmt(scope);
		add(stmt);
		return stmt;
	}

	public ForPropsStmt forProps(String takes, JSExpr fields) {
		ForPropsStmt ret = new ForPropsStmt(scope, takes, fields);
		add(ret);
		return ret;
	}

	public FunctionCall voidCall(String name, JSExpr... args) {
		FunctionCall toAdd = new FunctionCall(scope, name);
		for (JSExpr e : args)
			toAdd.arg(e);
		add(toAdd);
		return toAdd;
	}

	public MethodCall voidMethod(JSExpr target, String name, JSExpr... args) {
		MethodCall toAdd = new MethodCall(scope, target, name);
		for (JSExpr e : args)
			toAdd.arg(e);
		add(toAdd);
		return toAdd;
	}

	public void voidStmt(JSExpr expr) {
		add(new VoidExprStmt(expr));
	}

	public ForEachStmt forEach(String var, JSExpr over) {
		ForEachStmt ret = new ForEachStmt(scope, var, over);
		add(ret);
		return ret;
	}

	public JSExpr value(String cvar) {
		return new JSValue(cvar);
	}

	public void returnVoid() {
		add(new JSReturn());
	}
}
