package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;

public class JavaSourceDirResource extends SolidResource {
	public JavaSourceDirResource(JarCommand jarCommand, JarBuildCommand jar, File dir) {
		super(jarCommand, dir);
	}

	@Override
	public String compareAs() {
		return "JavaSource["+relative+"]";
	}
}
