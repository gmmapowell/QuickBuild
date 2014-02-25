package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.List;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.utils.FileUtils;

public class JavaSourceDirResource extends SolidResource {
	private final List<File> sourceFiles;
	private JarResource jarResource;

	public JavaSourceDirResource(File dir, List<File> sourceFiles) {
		super(null, dir);
		this.sourceFiles = sourceFiles;
	}

	@Override
	public String compareAs() {
		return "JavaSource["+FileUtils.posixPath(relative)+"]";
	}

	public List<File> getSources() {
		return sourceFiles;
	}

	public void buildsInto(JarResource jarResource) {
		this.jarResource = jarResource;
	}
	
	public JarResource getJarResource() {
		return jarResource;
	}
}
