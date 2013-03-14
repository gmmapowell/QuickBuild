package com.gmmapowell.bytecode;

public interface ByteCodeHolder {

	ByteCodeSink newEntry(String className, int destinations);

	void close();

	void addEntry(String className, ByteCodeCreator ret);

}
