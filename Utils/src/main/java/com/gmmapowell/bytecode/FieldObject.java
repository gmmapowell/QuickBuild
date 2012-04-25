package com.gmmapowell.bytecode;

public class FieldObject {
	private final boolean isStatic;
	private final String inClz;
	private final JavaType type;
	private final String name;

	public FieldObject(boolean isStatic, String inClz, JavaType javaType, String name) {
		this.isStatic = isStatic;
		this.inClz = inClz;
		this.type = javaType;
		this.name = name;
	}

	public FieldExpr use(NewMethodDefiner meth)
	{
		if (!isStatic)
			return new FieldExpr(meth, meth.myThis(), inClz, type, name);
		else
			return new FieldExpr(meth, null, inClz, type, name);
	}
	
	public FieldExpr useOn(NewMethodDefiner meth, Expr obj)
	{
		return new FieldExpr(meth, obj, inClz, type, name);
	}
}
