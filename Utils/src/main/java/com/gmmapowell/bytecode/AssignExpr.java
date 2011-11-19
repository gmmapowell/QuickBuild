package com.gmmapowell.bytecode;

public class AssignExpr extends Expr {
	private final Var assignTo;
	private final FieldExpr field;
	private final Expr expr;

	public AssignExpr(MethodCreator meth, Var assignTo, Expr expr) {
		super(meth);
		this.assignTo = assignTo;
		this.field = null;
		this.expr = expr;
	}

	public AssignExpr(MethodCreator meth, FieldExpr field, Expr expr) {
		super(meth);
		this.assignTo = null;
		this.field = field;
		this.expr = expr;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		if (field != null)
			field.prepare(meth);
		expr.spitOutByteCode(meth);
		if (assignTo != null)
			assignTo.store();
		else
			field.put(meth);
	}

}