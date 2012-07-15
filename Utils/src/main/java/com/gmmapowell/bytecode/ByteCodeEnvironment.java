package com.gmmapowell.bytecode;

import java.util.HashMap;
import java.util.Map;

public class ByteCodeEnvironment {
	private final Map<String, ByteCodeCreator> classes = new HashMap<String, ByteCodeCreator>();
	
	public void associate(ByteCodeCreator byteCodeCreator) {
		classes.put(byteCodeCreator.getCreatedName(), byteCodeCreator);
	}

	public ByteCodeCreator get(String name) {
		return classes.get(name);
	}

}
