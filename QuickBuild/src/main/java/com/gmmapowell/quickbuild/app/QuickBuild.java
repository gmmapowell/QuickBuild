package com.gmmapowell.quickbuild.app;

import java.io.File;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.SignificantWhiteSpaceFileReader;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.build.BuildContext;
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
		
		Config conf = new Config(new File(arguments.file).getParentFile());
		SignificantWhiteSpaceFileReader.read(conf, configFactory, arguments.file);
		conf.done();
		System.out.println("Configuration:");
		System.out.print(conf);
		
		// now we need to read back anything we've cached ...
		
		// now we try and build stuff ...
		System.out.println("");
		System.out.println("Building ...");
		BuildContext cxt = new BuildContext(conf);
		List<BuildCommand> cmds = conf.getBuildCommandsInOrder();
		int cnt = 0;
		int failures = 0;
		while (cnt < cmds.size())
		{
			BuildCommand bc = cmds.get(cnt);
			System.out.println((cnt+1) + ": " + bc);
			if (!cxt.execute(bc))
			{
				System.out.println("  Failed ... retrying");
				if (++failures > 3)
					throw new UtilException("The command " + bc + " failed 5 times in a row");
				continue;
			}
			failures = 0;
			cnt++;
		}
		cxt.showDependencies();
	}
}
