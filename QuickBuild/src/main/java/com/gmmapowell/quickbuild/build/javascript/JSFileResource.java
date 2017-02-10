package com.gmmapowell.quickbuild.build.javascript;

import java.io.File;

import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.core.SolidResource;
import com.gmmapowell.quickbuild.core.Tactic;

public class JSFileResource extends SolidResource {

	public JSFileResource(Tactic t, File resourceFile) {
		super(t, resourceFile);
	}

	@Override
	public String compareAs() {
		return "JS[" + FileUtils.posixPath(relative) + "]";
	}
}
