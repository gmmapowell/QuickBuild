package com.gmmapowell.bytecode;


public class UnboxExpr extends Expr {
	private final Expr expr;
	private final String outType;
	private final String convMethod;
	private final boolean protectFromNulls;

	public UnboxExpr(MethodCreator meth, Expr expr, boolean protectFromNulls) {
		super(meth);
		this.expr = expr;
		this.protectFromNulls = protectFromNulls;
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
		else if (expr.getType().equals("java.lang.Long"))
		{
			outType = "long";
			convMethod = "longValue";
		}
		else
		{
			outType = null;
			convMethod = null;
		}
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		expr.spitOutByteCode(meth);
		if (outType != null) {
			if (protectFromNulls) {
//				method.lenientMode(true);
				Var v = meth.varOfType(expr.getType(), "unbox");
				v.store();
				v.spitOutByteCode(meth);
			
				Marker doRet = meth.ifnonnull();
				if (outType.equals("int") || outType.equals("boolean")) {
					meth.iconst(0);
					meth.ireturn();
				} else if (outType.equals("long")) {
					meth.lconst(0);
					meth.lreturn();
				} else if (outType.equals("double")) {
					meth.dconst(0);
					meth.dreturn();
				}
				doRet.setHere();
				v.spitOutByteCode(meth);
			}
			meth.invokeVirtualMethod(expr.getType(), outType, convMethod);
		}
	}

	@Override
	public String getType() {
		if (outType == null)
			return expr.getType();
		return outType;
	}

}
