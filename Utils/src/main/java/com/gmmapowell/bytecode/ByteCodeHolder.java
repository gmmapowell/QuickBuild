package com.gmmapowell.bytecode;

public interface ByteCodeHolder {

	ByteCodeSink newEntry(String className, int destinations);

}
