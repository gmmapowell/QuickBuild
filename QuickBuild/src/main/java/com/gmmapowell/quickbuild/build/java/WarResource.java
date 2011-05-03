package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class WarResource extends SolidResource {
	public WarResource(Strategem parent, File f) {
		super(parent, f);
	}
	
	public File getFile() {
		return relative;
	}

	@Override
	public String toString() {
		return "War["+relative+"]";
	}


	@Override
	public String compareAs() {
		return "War[" + relative + "]";
	}
}
