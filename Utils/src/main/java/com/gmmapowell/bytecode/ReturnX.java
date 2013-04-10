package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class ReturnX extends Expr {

	private final String retType;
	private final Expr value;

	public ReturnX(MethodDefiner methodCreator, String retType, Expr value) {
		super(methodCreator);
		this.retType = retType;
		this.value = value;
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		if (retType.equals("void"))
		{
			meth.vreturn();
			return;
		}
		this.value.spitOutByteCode(meth);
		if (retType.equals("object"))
		{
			meth.areturn();
			return;
		}
		if (!retType.equals(value.getType()))
			throw new UtilException("Mismatched argument types in return: " + retType + " and " + value.getType());
		if (retType.equals("int") || retType.equals("boolean"))
			meth.ireturn();
		else if (retType.equals("long"))
			meth.lreturn();
		else if (retType.equals("double"))
			meth.dreturn();
		else
			meth.areturn();
	}

	@Override
	public String getType() {
		throw new UtilException("Don't ever use this as an argument");
	}

}
