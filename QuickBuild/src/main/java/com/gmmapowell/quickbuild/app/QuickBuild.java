package com.gmmapowell.quickbuild.app;

import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.ProcessArgs;
import com.gmmapowell.utils.SignificantWhiteSpaceFile;

public class QuickBuild {
	private static ArgumentDefinition[] argumentDefinitions = new ArgumentDefinition[] {
		new ArgumentDefinition("*.qb", Cardinality.REQUIRED, "file", "configuration file")
	};

	private static Arguments config;
	
	public static void main(String[] args)
	{
		ProcessArgs.process(config, argumentDefinitions, args);
		SignificantWhiteSpaceFile.read(Config.class, config.file);
	}
}
