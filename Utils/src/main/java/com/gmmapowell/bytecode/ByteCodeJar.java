package com.gmmapowell.bytecode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;

import com.gmmapowell.exceptions.UtilException;

public class ByteCodeJar implements ByteCodeHolder {
	private List<ByteCodeCreator> files = new ArrayList<ByteCodeCreator>();
	private final ByteCodeEnvironment env;

	public ByteCodeJar(ByteCodeEnvironment env) {
		this.env = env;
	}

	public ByteCodeSink newEntry(String className, int destinations) {
		ByteCodeCreator ret = new ByteCodeCreator(env, className);
		addEntry(className, ret);
		return ret;
	}

	public void addEntry(String className, ByteCodeCreator ret) {
		for (ByteCodeCreator bcc : files)
			if (bcc.getCreatedName().equals(className))
				throw new UtilException("Duplicated JAR entry: " + className);
		files.add(ret);
	}

	public void write(File writeTo) {
		try
		{
			JarOutputStream jos = new JarOutputStream(new FileOutputStream(writeTo));
			for (ByteCodeCreator bcc : files)
				bcc.addToJar(jos);
			jos.close();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public boolean hasEntries() {
		return !files.isEmpty();
	}

	@Override
	public void close() {
		files.clear();
	}
}
