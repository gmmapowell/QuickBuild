package com.gmmapowell.bytecode;

public class BCEClassLoader extends ClassLoader {
	private final ByteCodeEnvironment bce;

	public BCEClassLoader(ByteCodeEnvironment bce) {
		this.bce = bce;
	}

	public Class<?> defineClass(String name) {
		byte[] bs = bce.get(name).generate();
		return super.defineClass(name, bs, 0, bs.length);
	}

}
