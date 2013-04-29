package com.gmmapowell.bytecode;

import java.util.HashMap;
import java.util.Map;

public class ByteCodeEnvironment {
	private final Map<String, ByteCodeCreator> classes = new HashMap<String, ByteCodeCreator>();
	private BCEClassLoader loader;
	
	public void associate(ByteCodeCreator byteCodeCreator) {
		classes.put(byteCodeCreator.getCreatedName(), byteCodeCreator);
	}

	public ByteCodeCreator get(String name) {
		return classes.get(name);
	}

	public BCEClassLoader getClassLoader() {
		if (loader == null)
			loader = new BCEClassLoader(this);
		return loader;
	}

}
