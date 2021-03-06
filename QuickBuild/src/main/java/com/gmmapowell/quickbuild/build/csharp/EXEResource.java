package com.gmmapowell.quickbuild.build.csharp;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;

public class EXEResource extends SolidResource {
	public EXEResource(DevenvCommand devenvCommand, File file) {
		super(devenvCommand, file);
	}

	@Override
	public String compareAs() {
		return "EXEResource[" + relative + "]";
	}

}
