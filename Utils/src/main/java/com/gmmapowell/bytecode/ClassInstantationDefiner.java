package com.gmmapowell.bytecode;

/** This class creates a simple class in bytecode which is the implementation of
 *  a simple parent (usually abstract) class (which must be provided).
 *  
 *  This allows the construction of one or more constructors, each of which needs to be
 *  provided with parameters, and the order in which the arguments are passed to the parent
 *  constructor.
 */
public class ClassInstantationDefiner {
	private final String name;

	public ClassInstantationDefiner(String pkg, String name, String qualifiedImplementing)
	{
		this.name = name;
	}
	
	public IConstructor addConstructor()
	{
		return new IConstructor(name);
	}
}
