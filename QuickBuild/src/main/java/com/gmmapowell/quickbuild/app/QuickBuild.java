package com.gmmapowell.quickbuild.app;

import com.gmmapowell.parser.SignificantWhiteSpaceFileReader;
import com.gmmapowell.quickbuild.config.Arguments;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.ProcessArgs;

public class QuickBuild {
	private static ArgumentDefinition[] argumentDefinitions = new ArgumentDefinition[] {
		new ArgumentDefinition("*.qb", Cardinality.REQUIRED, "file", "configuration file")
	};

	private static Arguments arguments;

	private static ConfigFactory configFactory = new ConfigFactory();
	
	public static void main(String[] args)
	{
		arguments = new Arguments();
		ProcessArgs.process(arguments, argumentDefinitions, args);
		Config conf = new Config();
		SignificantWhiteSpaceFileReader.read(conf, configFactory, arguments.file);
		System.out.println(conf);
	}
}
