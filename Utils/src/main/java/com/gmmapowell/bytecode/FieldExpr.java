package com.gmmapowell.bytecode;

public class FieldExpr extends Expr {
	private final Var from;
	private final String clzName;
	private final String type;
	private final String fieldName;

	public FieldExpr(MethodCreator meth, Var from, String clzName, String type, String named) {
		super(meth);
		this.from = from;
		this.clzName = clzName;
		this.type = type;
		this.fieldName = named;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		if (from == null) { // static
			meth.getStatic(clzName, type, fieldName);
		}
		else
		{
			from.spitOutByteCode(meth);
			meth.getField(clzName, type, fieldName);
		}
	}
	
	public void prepare(MethodCreator meth) {
		if (from != null)
			from.spitOutByteCode(meth);
	}

	public void put(MethodCreator meth) {
		if (from == null)
			meth.putStatic(clzName, type, fieldName);
		else
			meth.putField(clzName, type, fieldName);
	}

	@Override
	public String getType() {
		return type;
	}
}
