package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class FieldExpr extends Expr {
	private final Expr from;
	private final String clzName;
	private final JavaType type;
	private final String fieldName;

	public FieldExpr(NewMethodDefiner meth, Expr from, String clzName, String type, String named) {
		this(meth, from, clzName, new JavaType(type), named);
	}

	public FieldExpr(NewMethodDefiner meth, Expr from, String clzName, JavaType type, String named) {
		super(meth);
		if (type == null)
			throw new UtilException("Type cannot be null");
		this.from = from;
		this.clzName = clzName;
		this.type = type;
		this.fieldName = named;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		if (from == null) { // static
			meth.getStatic(clzName, type.getActual(), fieldName);
		}
		else
		{
			from.spitOutByteCode(meth);
			meth.getField(clzName, type.getActual(), fieldName);
		}
	}
	
	public void prepare(MethodDefiner meth) {
		if (from != null)
			from.spitOutByteCode(meth);
	}

	public void put(MethodDefiner meth) {
		if (from == null)
			meth.putStatic(clzName, type.getActual(), fieldName);
		else
			meth.putField(clzName, type.getActual(), fieldName);
	}

	public boolean isPrimitive() {
		return type.isPrimitive();
	}
	
	@Override
	public String getType() {
		return type.getActual();
	}
	
	@Override
	public String toString() {
		return "Field["+clzName+":"+fieldName+" " + type+"]";
	}
}
