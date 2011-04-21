package com.gmmapowell.quickbuild.app;

import java.io.File;
import com.gmmapowell.parser.SignificantWhiteSpaceFileReader;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.java.JavaNature;
import com.gmmapowell.quickbuild.config.Arguments;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.ProcessArgs;

public class QuickBuild {
	private static ArgumentDefinition[] argumentDefinitions = new ArgumentDefinition[] {
		new ArgumentDefinition("*.qb", Cardinality.REQUIRED, "file", "configuration file"),
		new ArgumentDefinition("--config-only", Cardinality.OPTION, "configOnly", null),
		new ArgumentDefinition("--build-all", Cardinality.OPTION, "buildAll", null),
		new ArgumentDefinition("-D*", Cardinality.ZERO_OR_MORE, "dirResources", "provide directory resource")
	};

	private static Arguments arguments;

	private static ConfigFactory configFactory = new ConfigFactory();
	
	public static void main(String[] args)
	{
		arguments = new Arguments();
		ProcessArgs.process(arguments, argumentDefinitions, args);
		
		File file = new File(arguments.file);
		Config conf = new Config(file.getParentFile(), FileUtils.dropExtension(file.getName()));
		File hostfile = FileUtils.relativePath(new File(FileUtils.getHostName() + ".host.qb"));
		if (hostfile.exists())
			SignificantWhiteSpaceFileReader.read(conf, configFactory, hostfile);
		SignificantWhiteSpaceFileReader.read(conf, configFactory, file);
		conf.done();
		System.out.println("Configuration:");
		System.out.print(conf);
			
		// now we need to read back anything we've cached ...
		BuildContext cxt = new BuildContext(conf);
		try
		{
			cxt.registerNature(JavaNature.class);
			cxt.configure();
			cxt.loadCache();
		}
		catch (QuickBuildCacheException ex) {
			// the cache failed to load because of inconsistencies or whatever
			// ignore it and try again
			System.out.println("Cache was out of date; ignoring");
		}

		System.out.println("---- Dependencies");
		System.out.print(cxt.printableDependencyGraph());
		System.out.println("----");
		if (arguments.configOnly)
			return;

		/* TODO: I like this, but it needs to be more general
		for (String s : arguments.dirResources)
		{
			System.out.println("Adding proj " + s);
			cxt.addBuiltResource(new DirectoryResource(null, new File(s.substring(2))));
		}
		*/
			
		// determine what we need to build from git ...
		/*
		if (!arguments.buildAll)
		{
			Set<Project> changedProjects = conf.projectsFor(GitHelper.dirtyProjects(conf.projectRoots()));
			System.out.println("");
			System.out.println("The following projects have changed in git:");
			for (Project p : changedProjects)
				System.out.println(p);

			// TODO: cxt.limitBuildTo(changedProjects);
		}
		*/
		
		// now we try and build stuff ...
		System.out.println("");
		System.out.println("Building ...");
		Tactic bc;
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
