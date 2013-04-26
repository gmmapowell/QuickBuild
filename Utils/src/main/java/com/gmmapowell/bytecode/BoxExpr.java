package com.gmmapowell.bytecode;

public class BoxExpr extends Expr {
	private final Expr expr;
	private final String outType;
	private final String inType;

	public BoxExpr(MethodCreator meth, Expr expr) {
		super(meth);
		this.expr = expr;
		if (expr.getType().equals("boolean"))
			outType = "java.lang.Boolean";
		else if (expr.getType().equals("double"))
			outType = "java.lang.Double";
		else if (expr.getType().equals("int"))
			outType = "java.lang.Integer";
		else if (expr.getType().equals("long"))
			outType = "java.lang.Long";
		else
			outType = null;
		if (outType != null)
			inType = expr.getType();
		else
			inType = null;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		expr.spitOutByteCode(meth);
		if (expr.getType().equals(inType))
			meth.invokeStatic(outType, outType, "valueOf", inType);
	}

	@Override
	public String getType() {
		return outType!=null?outType:expr.getType();
	}

}
