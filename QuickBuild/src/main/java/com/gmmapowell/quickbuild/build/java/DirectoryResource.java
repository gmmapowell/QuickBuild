package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;

public class DirectoryResource extends SolidResource {
	public DirectoryResource(File f) {
		super(null, f);
	}

	@Override
	public String compareAs() {
		return "Directory["+relative+"]";
	}
}
