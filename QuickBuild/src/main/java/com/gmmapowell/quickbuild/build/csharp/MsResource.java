package com.gmmapowell.quickbuild.build.csharp;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;

/**
 * I can't help feeling this is a hack, like DirectoryResource was for Java.
 * I think we should have separate XAP, EXE, blah, blah, targets
 * And we should have the smarts here to know the file structure and everything.
 *
 * <p>
 * &copy; 2011 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class MsResource extends SolidResource {

	private final String projectName;

	public MsResource(DevenvCommand devenvCommand, File rootdir, String projectName) {
		super(devenvCommand, rootdir);
		this.projectName = projectName;
	}

	@Override
	public String compareAs() {
		return "MsResource[" + projectName + "]";
	}
}
