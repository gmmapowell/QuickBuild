package com.gmmapowell.quickbuild.app;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.gmmapowell.git.GitHelper;
import com.gmmapowell.git.GitRecord;
import com.gmmapowell.parser.SignificantWhiteSpaceFileReader;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildExecutor;
import com.gmmapowell.quickbuild.config.Arguments;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.DateUtils;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.utils.ProcessArgs;

public class QuickBuild {
	private static ArgumentDefinition[] argumentDefinitions = new ArgumentDefinition[] {
		new ArgumentDefinition("*.qb", Cardinality.REQUIRED, "file", "configuration file"),
		new ArgumentDefinition("--args", Cardinality.LIST, "showArgsFor", null),
		new ArgumentDefinition("--blank", Cardinality.OPTION, "blank", "blank memory"),
		new ArgumentDefinition("--build-all", Cardinality.OPTION, "buildAll", null),
		new ArgumentDefinition("--cache", Cardinality.OPTION, "cachedir", "Cache directory"),
		new ArgumentDefinition("--config-only", Cardinality.OPTION, "configOnly", null),
		new ArgumentDefinition("--debug", Cardinality.LIST, "showDebugFor", null),
		new ArgumentDefinition("--debugInternals", Cardinality.LIST, "debug", null),
		new ArgumentDefinition("--quiet", Cardinality.LIST, "quiet", "super quiet mode")
	};

	private static Arguments arguments;

	private static ConfigFactory configFactory = new ConfigFactory();
	
	public static void main(String[] args)
	{
		Date launched = new Date();
		arguments = new Arguments();
		ProcessArgs.process(arguments, argumentDefinitions, args);
		
//		System.out.println("user.home = " + System.getProperty("user.home"));
		File file = new File(arguments.file);
		OrderedFileList ofl = new OrderedFileList(FileUtils.relativePath(file));
		Config conf = new Config(configFactory, file.getParentFile(), FileUtils.dropExtension(file.getName()), arguments.cachedir);
		{
			File hostfile = new File(file.getParentFile(), FileUtils.getHostName() + ".host.qb");
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
		configFactory.done();
		if (arguments.debug)
		{
			System.out.println("Files read (in order):");
			for (File f : ofl)
			{
				String path = f.getPath();
				try
				{
					path = f.getCanonicalPath();
				}
				catch (IOException ex)
				{
				}
				System.out.println("  " + path);
			}
			System.out.println();
			System.out.println("Configuration:");
			System.out.print(conf);
		}
			
		boolean buildAll = arguments.buildAll;
		boolean blankMemory = arguments.blank;
		if (!arguments.quiet)
			System.out.println("Comparing files ...");
		GitRecord mainFiles = GitHelper.checkFiles(true, ofl, new File(conf.getCacheDir(), file.getName()));
		blankMemory |= mainFiles.isDirty();
		buildAll |= mainFiles.isDirty();
		
		// now we need to read back anything we've cached ...
		BuildContext cxt = new BuildContext(conf, configFactory, blankMemory, buildAll, arguments.debug, arguments.showArgsFor, arguments.showDebugFor, arguments.quiet);
		cxt.configure();
		
		if (!arguments.quiet)
			System.out.println();

		if (arguments.configOnly)
		{
			System.out.println("---- Dependencies");
			System.out.print(cxt.printableDependencyGraph());
			System.out.println("----");
			System.out.println("---- BuildOrder");
			System.out.print(cxt.printableBuildOrder(true));
			System.out.println("----");

			return;
		}
		if (arguments.debug) {
			System.out.println("Predicted Build Order:");
			System.out.print(cxt.printableBuildOrder(false));
			System.out.println();
		}
		
		if (!arguments.quiet)
			System.err.println("Pre-build configuration time: " + DateUtils.elapsedTime(launched, new Date(), DateUtils.Format.hhmmss3));
		
		mainFiles.commit();
		cxt.getBuildOrder().commitUnbuilt();
		new BuildExecutor(cxt, arguments.debug).doBuild();
	}
}
