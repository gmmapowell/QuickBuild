package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Strategem;

public class StrategemResource extends SolidResource {

	private boolean clean = true;

	public StrategemResource(Strategem builtBy) {
		super(builtBy, getRoot(builtBy));
	}

	private static File getRoot(Strategem builtBy) {
		if (builtBy == null)
			throw new UtilException("There was no strategem specified");
		if (builtBy.rootDirectory() == null)
			throw new UtilException("The strategem " + builtBy + " did not have a root directory");
		return builtBy.rootDirectory();
	}

	@Override
	public String compareAs() {
		return "Strategem["+relative+"]";
	}

	public boolean isClean() {
		return clean;
	}

	public void markDirty() {
		clean = false;
	}
}
