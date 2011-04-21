package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class JarResource extends SolidResource {
	public JarResource(Strategem parent, File f) {
		super(parent, f);
	}
	
	public File getFile() {
		return relative;
	}

	@Override
	public String toString() {
		return "Jar["+relative+"]";
	}


	@Override
	public String compareAs() {
		return "Jar[" + relative + "]";
	}
}
