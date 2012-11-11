package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Tactic;

public class WarResource extends SolidResource {
	public WarResource(Tactic parent, File f) {
		super(parent, f);
	}
	
	public File getFile() {
		return relative;
	}

	@Override
	public BuildResource cloneInto(CloningResource to) {
		return new WarResource(to.getBuiltBy(), to.getClonedPath());
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
