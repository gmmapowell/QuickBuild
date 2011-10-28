package com.gmmapowell.bytecode;

public class JavaType extends JavaInfo {
	public static JavaType string = new JavaType("java.lang.String");
	private final String actual;
	private final String[] generics;

	public JavaType(String actual, String... generics) {
		this.actual = actual;
		this.generics = generics;
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
		for (String s : generics)
		{
			sb.insert(sb.length()-1, "<");
			sb.insert(sb.length()-1, JavaInfo.map(s));
			sb.insert(sb.length()-1, ">");
		}
		return sb.toString();
	}

	public String getActual() {
		return actual;
	}

	public boolean isGeneric() {
		return generics != null && generics.length > 0;
	}

}