package com.gmmapowell.bytecode;

/** Define a constructor, which is a kind of method
 * This has all the arguments to the constructor handled by the
 * method object; this is responsible for calling the right parent
 * constructor with the right selection of arguments in the right order.
 */
public class IConstructor extends IMethod {

	IConstructor(String clzName) {
		super(null); // what exactly does a constructor want?
	}
	
	// Specify the order in which the argument should be used to pass to the parent constructor
	public void useArgument(String qualifiedParentType, String name)
	{
	}
}
