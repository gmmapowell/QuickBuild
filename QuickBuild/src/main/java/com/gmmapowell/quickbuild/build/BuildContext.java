package com.gmmapowell.quickbuild.build;

import java.util.HashMap;
import java.util.Map;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class BuildContext {
	private final Config conf;
	private Map<String, String> availablePackages = new HashMap<String, String>();

	public BuildContext(Config conf) {
		this.conf = conf;
		conf.supplyPackages(availablePackages);
	}

	// TODO: this is more general than just a java build command, but what?
	public void addDependency(JavaBuildCommand javaBuildCommand, String needsJavaPackage) {
		if (!availablePackages.containsKey(needsJavaPackage))
			throw new QuickBuildException("There is no java package " + needsJavaPackage);
	}

}
