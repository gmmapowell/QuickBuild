package com.gmmapowell.bytecode;

import com.gmmapowell.bytecode.Var.AVar;

public interface NewMethodDefiner {

	void throwsException(String exception);

	Var argument(String type, String aname);

	Var getArgument(int i);

	Var varOfType(String type, String aname);

	AVar myThis();

	Var saveAslocal(String clz, String name);

	Var avar(JavaType clz, String name);

	Var avar(String clz, String name);

	Var ivar(String clz, String name);

	Var dvar(String clz, String name);

	Expr aNull();

	int argCount();

	FieldExpr getField(String name);
	FieldExpr getField(Expr on, String name);

	Expr staticField(String clz, String type, String named);

	Expr as(Expr expr, String newType);

	AssignExpr assign(Var assignTo, Expr expr);

	Expr assign(FieldExpr field, Expr expr);

	BoolConstExpr boolConst(boolean b);

	/** Group a collection of expressions as a block */
	Expr block(Expr... exprs);

	Expr callSuper(String returns, Expr obj, String parentClzName,
			String methodName, Expr... args);

	Expr callVirtual(String returns, Expr obj, String methodName, Expr... args);

	Expr callInterface(String returns, Expr obj, String methodName,
			Expr... args);

	Expr callStatic(String inClz, String returns, String methodName,
			Expr... args);

	Expr castTo(Expr expr, String ofType);

	ClassConstExpr classConst(String cls);

	Expr concat(Object... args);

	IfExpr ifBoolean(Expr expr, Expr then, Expr orelse);

	Expr ifEquals(Expr left, Expr right, Expr then, Expr orelse);

	Expr ifNotNull(Expr test, Expr then, Expr orelse);

	Expr ifNull(Expr test, Expr then, Expr orelse);

	Expr isNull(Expr test, Expr yes, Expr no);

	IntConstExpr intConst(int i);

	MakeNewExpr makeNew(String ofClz, Expr... args);

	Expr returnVoid();

	Expr returnBool(Expr i);

	Expr returnInt(Expr i);

	Expr returnObject(Expr e);

	StringConstExpr stringConst(String str);

	Expr throwException(String clz, Expr... args);

	Expr trueConst();

	Expr voidExpr(Expr ignoredResult);

	void lenientMode(boolean mode);

	Expr box(Expr expr);

	Expr unbox(Expr expr);
}
