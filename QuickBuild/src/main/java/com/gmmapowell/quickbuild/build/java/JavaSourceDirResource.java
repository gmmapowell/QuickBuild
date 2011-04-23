package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class JavaSourceDirResource extends SolidResource {
	public JavaSourceDirResource(Strategem builder, File dir) {
		super(builder, dir);
	}

	@Override
	public String compareAs() {
		return "JavaSource["+relative+"]";
	}
}
