package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.List;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class JavaSourceDirResource extends SolidResource {
	private final List<File> sourceFiles;

	public JavaSourceDirResource(Strategem builder, File dir, List<File> sourceFiles) {
		super(builder, dir);
		this.sourceFiles = sourceFiles;
	}

	@Override
	public String compareAs() {
		return "JavaSource["+relative+"]";
	}

	public List<File> getSources() {
		return sourceFiles;
	}
}
