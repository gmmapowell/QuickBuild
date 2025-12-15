package com.gmmapowell.quickbuild.build.bash;

import java.io.File;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Tactic;

public class ScriptResource extends SolidResource {
	private String identifier;

	public ScriptResource(Tactic tactic, File path) {
		super(tactic, path);
		this.identifier = "Script["+path+"]";
	}

	@Override
	public String compareAs() {
		return identifier;
	}
}
