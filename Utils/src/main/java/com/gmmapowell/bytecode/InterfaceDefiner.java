package com.gmmapowell.bytecode;

/** This defines (from scratch) an interface 
 */
public class InterfaceDefiner {
	public InterfaceDefiner(String pkg, String name)
	{
	}
	
	public void extendsInterface(String qualifiedType)
	{
	}

	public void addProperty(String qualifiedType, String name)
	{
	}
	
	public IMethod addAbstractMethod(String name)
	{
		IMethod ret = new IMethod(name);
		return ret;
	}
	
	// TODO: annotations?
	
	public byte[] bytecodes()
	{
		throw new UnsupportedOperationException();
	}
}
