package com.gmmapowell.bytecode;

public class IMethod {
	IMethod(String name)
	{
	}
	
	public IArgument addArgument(String qualifiedClass, String name)
	{
		IArgument ret = new IArgument(qualifiedClass, name);
		return ret;
	}
}
