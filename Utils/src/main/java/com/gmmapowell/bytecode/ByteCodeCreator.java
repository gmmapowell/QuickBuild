package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class ByteCodeCreator {
	private ByteCodeFile bcf = new ByteCodeFile();
	private String name;
	private String pkg;
	private final File file;

	public ByteCodeCreator(File file) {
		this.file = file;
		pkg = FileUtils.getPackage(file);
		name = FileUtils.getUnextendedName(file);
		bcf.thisClass(FileUtils.convertToDottedNameDroppingExtension(file));
	}

	public void superclass(String string) {
		bcf.superClass(string);
	}

	public void addToJar(JarOutputStream jos) {
		try {
			JarEntry je = new JarEntry(file.getPath());
			jos.putNextEntry(je);
			generateByteCodes(jos);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
	}

	private void generateByteCodes(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		bcf.write(dos);
		dos.flush();
	}

	public MethodCreator method(boolean isStatic, String string) {
		MethodCreator ret = new MethodCreator(bcf, isStatic, string);
		bcf.addMethod(ret);
		return ret;
	}

}
