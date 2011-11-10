package com.gmmapowell.quickbuild.build.csharp;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class EXEResource extends SolidResource {
	public EXEResource(DevenvCommand devenvCommand, File file) {
		super(devenvCommand, file);
	}

	@Override
	public String compareAs() {
		return "EXEResource[" + relative + "]";
	}

}
