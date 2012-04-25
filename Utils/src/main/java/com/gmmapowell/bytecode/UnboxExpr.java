package com.gmmapowell.bytecode;

public class UnboxExpr extends Expr {
	private final Expr expr;
	private final String outType;
	private final String inType;
	private final String convMethod;

	public UnboxExpr(MethodCreator meth, Expr expr) {
		super(meth);
		this.expr = expr;
		if (expr.getType().equals("java.lang.Boolean"))
		{
			outType = "boolean";
			convMethod = "booleanValue";
		}
		else if (expr.getType().equals("java.lang.Double"))
		{
			outType = "double";
			convMethod = "doubleValue";
		}
		else if (expr.getType().equals("java.lang.Integer"))
		{
			outType = "int";
			convMethod = "intValue";
		}
		else
		{
			outType = null;
			convMethod = null;
		}
		if (outType != null)
			inType = expr.getType();
		else
			inType = null;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		expr.spitOutByteCode(meth);
		if (expr.getType().equals(inType))
			meth.invokeVirtualMethod(inType, outType, convMethod);
	}

	@Override
	public String getType() {
		if (outType == null)
			return expr.getType();
		return outType;
	}

}
