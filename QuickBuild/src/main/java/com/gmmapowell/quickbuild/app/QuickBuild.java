package com.gmmapowell.quickbuild.app;

import java.io.File;
import java.util.Set;

import com.gmmapowell.git.GitHelper;
import com.gmmapowell.parser.SignificantWhiteSpaceFileReader;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Arguments;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.ProcessArgs;

public class QuickBuild {
	private static ArgumentDefinition[] argumentDefinitions = new ArgumentDefinition[] {
		new ArgumentDefinition("*.qb", Cardinality.REQUIRED, "file", "configuration file"),
		new ArgumentDefinition("--config-only", Cardinality.OPTION, "configOnly", null),
		new ArgumentDefinition("--build-all", Cardinality.OPTION, "buildAll", null)
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
		
		if (arguments.configOnly)
			return;
		
		// now we need to read back anything we've cached ...
		BuildContext cxt = new BuildContext(conf);
		cxt.loadCache();
		
		// determine what we need to build from git ...
		if (!arguments.buildAll)
		{
			Set<Project> changedProjects = conf.projectsFor(GitHelper.dirtyProjects(conf.projectRoots()));
			System.out.println("");
			System.out.println("The following projects have changed in git:");
			for (Project p : changedProjects)
				System.out.println(p);

			cxt.limitBuildTo(changedProjects);
		}
		
		// now we try and build stuff ...
		System.out.println("");
		System.out.println("Building ...");
		BuildCommand bc;
		while ((bc = cxt.next())!= null)
		{
			BuildStatus outcome = cxt.execute(bc);
			if (!outcome.isGood())
			{
				cxt.buildFail(outcome);
				if (outcome.isBroken())
				{
					System.out.println("Aborting build due to failure");
					break;
				}
				else if (outcome.tryAgain())
				{
					System.out.println("  Failed ... retrying");
					cxt.tryAgain();
					continue;
				}
				// else move on ...
			}
			cxt.advance();
		}
		cxt.saveDependencies();
		cxt.showAnyErrors();
	}
}
