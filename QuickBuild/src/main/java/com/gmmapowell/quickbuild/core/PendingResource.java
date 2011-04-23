package com.gmmapowell.quickbuild.core;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class PendingResource extends SolidResource {
	private String pendingName;

	public PendingResource(String from) {
		super(null, new File(FileUtils.getCurrentDir(), "unused"));
		this.pendingName = from;
	}
	
	@Override
	public File getPath() {
		throw new UtilException("Cannot use a PendingResource");
	}

	@Override
	public String compareAs() {
		return pendingName;
	}
}
