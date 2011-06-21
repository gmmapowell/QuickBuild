package com.gmmapowell.bytecode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;

import com.gmmapowell.exceptions.UtilException;

public class ByteCodeJar {

	private List<ByteCodeCreator> files = new ArrayList<ByteCodeCreator>();

	public ByteCodeJar() {
		// sort out something about the manifest
	}

	public ByteCodeCreator newEntry(String string) {
		System.out.println("Creating new class " + string);
		ByteCodeCreator ret = new ByteCodeCreator(string);
		files.add(ret);
		return ret;
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

}
