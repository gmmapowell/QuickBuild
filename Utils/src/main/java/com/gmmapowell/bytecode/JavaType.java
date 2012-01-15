package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class JavaType extends JavaInfo {
	public static final JavaType date = new JavaType("java.util.Date");
	public static final JavaType double_ = new JavaType("double");
	public static final JavaType int_ = new JavaType("int");
	public static final JavaType string = new JavaType("java.lang.String");
	private final String actual;
	private final String[] generics;

	public JavaType(String actual, String... generics) {
		this.actual = actual;
		this.generics = generics;
		for (String s : generics)
			if (s == null)
				throw new UtilException("Null generic passed to " + actual);
	}

	public String asSignature()
	{
		return map(actual);
	}
	
	public String asGeneric()
	{
		if (generics == null || generics.length == 0)
			return asSignature();
		StringBuilder sb = new StringBuilder();
		sb.append(JavaInfo.map(actual));
		sb.insert(sb.length()-1, "<");
		for (String s : generics)
		{
			sb.insert(sb.length()-1, JavaInfo.map(s));
		}
		sb.insert(sb.length()-1, ">");
		return sb.toString();
	}

	public String getActual() {
		return actual;
	}

	public boolean isGeneric() {
		return generics != null && generics.length > 0;
	}

}
