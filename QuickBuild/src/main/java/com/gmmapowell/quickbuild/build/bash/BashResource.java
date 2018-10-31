package com.gmmapowell.quickbuild.build.bash;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;

public class BashResource extends SolidResource {
	private String identifier;

	public BashResource(BashCommand bash, File path, String identifier) {
		super(bash, path);
		this.identifier = identifier;
	}

	@Override
	public String compareAs() {
		return identifier;
	}
}
