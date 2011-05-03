package com.gmmapowell.quickbuild.app;

import java.io.File;

import com.gmmapowell.git.GitHelper;
import com.gmmapowell.parser.SignificantWhiteSpaceFileReader;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Arguments;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildCacheException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.utils.ProcessArgs;

public class QuickBuild {
	private static ArgumentDefinition[] argumentDefinitions = new ArgumentDefinition[] {
		new ArgumentDefinition("*.qb", Cardinality.REQUIRED, "file", "configuration file"),
		new ArgumentDefinition("--config-only", Cardinality.OPTION, "configOnly", null),
		new ArgumentDefinition("--build-all", Cardinality.OPTION, "buildAll", null),
		new ArgumentDefinition("--args", Cardinality.LIST, "showArgsFor", null),
		new ArgumentDefinition("--debug", Cardinality.LIST, "showDebugFor", null)
	};

	private static Arguments arguments;

	private static ConfigFactory configFactory = new ConfigFactory();
	
	public static void main(String[] args)
	{
		arguments = new Arguments();
		ProcessArgs.process(arguments, argumentDefinitions, args);
		
		File file = new File(arguments.file);
		OrderedFileList ofl = new OrderedFileList(FileUtils.relativePath(file));
		Config conf = new Config(file.getParentFile(), FileUtils.dropExtension(file.getName()));
		{
			File hostfile = FileUtils.relativePath(new File(FileUtils.getHostName() + ".host.qb"));
			if (hostfile.exists())
			{
				SignificantWhiteSpaceFileReader.read(conf, configFactory, hostfile);
				ofl.add(hostfile);
			}
		}
		{
			File roothostfile = new File(new File(System.getProperty("user.home")), ".qbinit." + FileUtils.getHostName());
			if (roothostfile.exists())
			{
				SignificantWhiteSpaceFileReader.read(conf, configFactory, roothostfile);
				ofl.add(roothostfile);
			}
		}
		{
			File rootfile = new File(new File(System.getProperty("user.home")), ".qbinit");
			if (rootfile.exists())
			{
				SignificantWhiteSpaceFileReader.read(conf, configFactory, rootfile);
				ofl.add(rootfile);
			}

		}
		SignificantWhiteSpaceFileReader.read(conf, configFactory, file);
		conf.done();
		System.out.println("Configuration:");
		System.out.print(conf);
			
		boolean buildAll = arguments.buildAll;
		buildAll |= GitHelper.checkFiles(true, ofl, new File(conf.getCacheDir(), file.getName()));
		
		// now we need to read back anything we've cached ...
		BuildContext cxt = new BuildContext(conf, configFactory, buildAll, arguments.showArgsFor, arguments.showDebugFor);
		try
		{
    		cxt.configure();
			cxt.loadCache();
		}
		catch (QuickBuildCacheException ex) {
			// the cache failed to load because of inconsistencies or whatever
			// ignore it and try again
			System.out.println("Cache was out of date; ignoring");
		}

		if (arguments.configOnly)
		{
			System.out.println("---- Dependencies");
			System.out.print(cxt.printableDependencyGraph());
			System.out.println("----");

			return;
		}

		/* TODO: I like this, but it needs to be more general
		for (String s : arguments.dirResources)
		{
			System.out.println("Adding proj " + s);
			cxt.addBuiltResource(new DirectoryResource(null, new File(s.substring(2))));
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
		cxt.saveBuildOrder();
		cxt.showAnyErrors();
	}
}
