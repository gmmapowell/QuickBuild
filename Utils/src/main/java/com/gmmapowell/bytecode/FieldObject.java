package com.gmmapowell.bytecode;

public class FieldObject {
	private final boolean isStatic;
	private final String inClz;
	private final String type;
	private final String name;

	public FieldObject(String inClz, String type, String name) {
		this.isStatic = false;
		this.inClz = inClz;
		this.type = type;
		this.name = name;
	}

	public FieldExpr use(MethodCreator meth)
	{
		if (!isStatic)
			return new FieldExpr(meth, meth.myThis(), inClz, type, name);
		else
			return new FieldExpr(meth, null, inClz, type, name);
	}
}
