package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;


public class JSBlock {
	private final List<Stmt> stmts = new ArrayList<Stmt>();
	private final JSScope scope;
	private final boolean useParens;

	public abstract class JSCompiler {
		public JSCompiler() {
			compile();
		}
		
		protected void assign(JSVar var, JSExpr rvalue) {
			stmts.add(new Assign(var, rvalue, false));
		}

		protected void blockComment(String text) {
			stmts.add(new JSBlockComment(text));
		}

		protected VarDecl declareVar(String var, JSExpr expr) {
			VarDecl decl = declareExactVar(var);
			decl.value(expr);
			return decl;
		}

		protected FunctionCall function(String function, JSExpr... args) {
			FunctionCall expr = new FunctionCall(scope, function);
			for (JSExpr arg : args)
				expr.arg(arg);
			return expr;
		}

		protected FunctionCall jquery(String s) {
			return function("$", string(s));
		}

		protected MethodCall methodExpr(String callOn, String method, JSExpr... args) {
			return methodExpr(scope.getDefinedVar(callOn), method, args);
		}
		
		protected MethodCall methodExpr(JSExpr callOn, String method, JSExpr... args) {
			JSExprGenerator gen = new JSExprGenerator(scope);
			MethodCall expr = gen.methodCall(callOn, method);
			for (JSExpr arg : args)
				expr.arg(arg);
			return expr;
		}

		protected JSObjectExpr objectHash() {
			JSExprGenerator gen = new JSExprGenerator(scope);
			return gen.literalObject();
		}

		protected JSExpr string(String s) {
			return new JSValue(s);
		}

		protected void voidMethod(String callOn, String method, JSExpr... args) {
			stmts.add(new VoidExprStmt(methodExpr(callOn, method, args)));
		}

		protected void voidMethod(JSExpr callOn, String method, JSExpr... args) {
			stmts.add(new VoidExprStmt(methodExpr(callOn, method, args)));
		}

		protected JSExpr This() {
			return new JSThis(scope);
		}

		public abstract void compile();
	}

	JSBlock(JSScope scope) {
		this(scope, true);
	}

	public JSBlock(JSScope scope, boolean useParens) {
		this.scope = scope;
		this.useParens = useParens;
	}

	public JSScope getScope() {
		return scope;
	}

	public LValue resolveClass(String name) {
		return scope.resolveClass(name);
	}

	/** For ordering purposes, it is desirable to be able to say at time t, "I may want to put things here"
	 * and then at time t+k to put them there.  This issues such a marker with the appearance of a block.
	 * @return a sub-block of this block
	 */
	public JSBlock marker() {
		JSInnerBlock ret = new JSInnerBlock(scope);
		stmts.add(ret);
		return ret.getBlock();
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

	public VarDecl declareExactVar(String var) {
		JSVar jsvar = scope.getExactVar(var);
		VarDecl ret = new VarDecl(scope, jsvar);
		add(new Assign(jsvar, ret, true));
		return ret;
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

	public FunctionCall voidCall(JSVar jsVar) {
		FunctionCall toAdd = new FunctionCall(scope, jsVar);
		add(toAdd);
		return toAdd;
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

	public JSExprGenerator voidStmt() {
		JSExprGenerator ret = new JSExprGenerator(scope);
		add(new VoidExprStmt(ret));
		return ret;
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

	public JSVar mapType(String name) {
		return scope.getExactVar(name);
	}
}
