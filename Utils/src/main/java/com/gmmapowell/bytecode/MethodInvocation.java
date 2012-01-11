package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class MethodInvocation extends Expr {

	private final String type;
	private final String returns;
	private final Expr obj;
	private final String methodName;
	private final Expr[] args;

	public MethodInvocation(MethodCreator methodCreator, String type, String returns, Expr obj, String parentClzName, String methodName, Expr[] args) {
		super(methodCreator);
		this.type = type;
		this.returns = returns;
		this.obj = obj;
		this.methodName = methodName;
		this.args = args;
	}

	@Override
	public void spitOutByteCode(MethodCreator meth) {
		if (obj != null)
			obj.spitOutByteCode(meth);
		String[] argTypes = new String[args.length];
		for (int i=0;i<args.length;i++)
		{
			Expr e = args[i];
			argTypes[i] = e.getType();
			e.spitOutByteCode(meth);
		}
		if (type.equals("virtual"))
			meth.invokeVirtualMethod(obj.getType(), returns, methodName, argTypes);
		else if (type.equals("interface"))
			meth.invokeInterface(obj.getType(), returns, methodName, argTypes);
		else if (type.equals("super"))
			meth.invokeParentMethod(returns, methodName, argTypes);
		else
			throw new UtilException("Can't handle method type " + type);
			
//		meth.invokeOtherConstructor(ofClz, argTypes);
	}

	@Override
	public String getType() {
		return returns;
	}

}
