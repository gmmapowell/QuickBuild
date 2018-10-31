package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.build.ExecutesInDirCommand;
import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Tactic;

public class JarDirectoryResource extends SolidResource {
	public JarDirectoryResource(Tactic t, File f) {
		super(t, figureAbsolutePath(t, f));
	}

	protected static File figureAbsolutePath(Tactic t, File f) {
		if (f.isAbsolute())
			return f;
		else if (t instanceof ExecutesInDirCommand)
			return new File(((ExecutesInDirCommand)t).getExecDir(), f.getPath()).getAbsoluteFile();
		else
			return new File(t.belongsTo().rootDirectory(), f.getPath()).getAbsoluteFile();
	}

	@Override
	public String compareAs() {
		return "Directory["+relative+"]";
	}
}
