package com.gmmapowell.bytecode;

import com.gmmapowell.exceptions.UtilException;

public class JavaType extends JavaInfo {
	public static final JavaType boolean_ = new JavaType("boolean");
	public static final JavaType byte_ = new JavaType("byte");
	public static final JavaType date = new JavaType("java.util.Date");
	public static final JavaType double_ = new JavaType("double");
	public static final JavaType int_ = new JavaType("int");
	public static final JavaType long_ = new JavaType("long");
	public static final JavaType string = new JavaType("java.lang.String");
	private final String actual;
	private final JavaType[] generics;
	private boolean extended;

	public JavaType(String actual)
	{
		this.actual = actual;
		this.generics = new JavaType[0];
	}
	
	public JavaType(String actual, String... generics) {
		this.actual = actual;
		this.generics = new JavaType[generics.length];
		int pos = 0;
		for (String s : generics)
			if (s == null)
				throw new UtilException("Null generic passed to " + actual);
			else
				this.generics[pos++] = new JavaType(s);
	}

	public JavaType(String actual, JavaType... generics) {
		this.actual = actual;
		this.generics = generics;
		for (JavaType gen : generics)
			if (gen == null)
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
		for (JavaType jt : generics)
		{
			if (jt.extended)
				sb.insert(sb.length()-1, "+");
			sb.insert(sb.length()-1, jt.asGeneric());
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

	public boolean equals(Object other)
	{
		return super.equals(other) || actual.equals(other);
	}

	public JavaType orExtended() {
		extended = true;
		return this;
	}

	public boolean isPrimitive() {
		return boolean_.equals(actual) || byte_.equals(actual) || double_.equals(actual) || int_.equals(actual) || long_.equals(actual); 
	}
	
	@Override
	public String toString() {
		return actual + (generics!=null&&generics.length>0?":"+generics.length:"");
	}
}
