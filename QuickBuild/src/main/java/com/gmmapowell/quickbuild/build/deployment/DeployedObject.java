package com.gmmapowell.quickbuild.build.deployment;

import java.io.File;

import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Tactic;
import org.zinutils.utils.FileUtils;

public class DeployedObject extends SolidResource implements BuildResource {

	private String name;

	public DeployedObject(Tactic builtBy, File path) {
		super(builtBy, path);
		name = FileUtils.ensureExtension(path.getPath(), ".__");
	}

	@Override
	public String compareAs() {
		return "DeployedTo[" + name + "]";
	}

}
