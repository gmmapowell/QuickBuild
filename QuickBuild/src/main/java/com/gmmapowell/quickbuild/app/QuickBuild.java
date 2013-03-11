package com.gmmapowell.quickbuild.app;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.gmmapowell.git.GitHelper;
import com.gmmapowell.git.GitRecord;
import com.gmmapowell.parser.SignificantWhiteSpaceFileReader;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildExecutor;
import com.gmmapowell.quickbuild.config.Arguments;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
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
		new ArgumentDefinition("--nthreads", Cardinality.OPTION, "nthreads", "number of threads"),
		new ArgumentDefinition("--quiet", Cardinality.LIST, "quiet", "super quiet mode"),
		new ArgumentDefinition("--teamcity", Cardinality.OPTION, "teamcity", "TeamCity integration mode"),
		new ArgumentDefinition("--upto", Cardinality.OPTION, "upTo", "last target to build")
	};

	private static Arguments arguments;

	private static ConfigFactory configFactory = new ConfigFactory();
	
	public static void main(String[] args)
	{
		try {
		List<File> pathElts = FileUtils.splitJavaPath(System.getProperty("java.class.path"));
		File utilsJar = null;
		for (File f : pathElts) {
			if (f.getName().endsWith("Utils.jar") || f.getPath().endsWith("Utils/bin"))
				utilsJar = f;
		}
		if (utilsJar == null)
			throw new QuickBuildException("Could not find Utils.jar on the class path");
		Date launched = new Date();
		arguments = new Arguments();
		ProcessArgs.process(arguments, argumentDefinitions, args);
		BuildOutput output = new BuildOutput(arguments.teamcity);
		output.openBlock("Config");

//		System.out.println("user.home = " + System.getProperty("user.home"));
		File file = new File(arguments.file);
		OrderedFileList ofl = new OrderedFileList(FileUtils.relativePath(file));
		Config conf = new Config(configFactory, output, file.getParentFile(), FileUtils.dropExtension(file.getName()), arguments.cachedir);
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
		output.closeBlock("Config");

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
		output.openBlock("compareFiles");
		if (!arguments.quiet)
			output.println("Comparing files ...");
		GitRecord mainFiles = GitHelper.checkFiles(true, ofl, new File(conf.getCacheDir(), file.getName()));
		blankMemory |= mainFiles.isDirty();
		buildAll |= mainFiles.isDirty();
		
		// now we need to read back anything we've cached ...
		BuildContext cxt = new BuildContext(conf, configFactory, output, blankMemory, buildAll, arguments.debug, arguments.showArgsFor, arguments.showDebugFor, arguments.quiet, utilsJar, arguments.upTo, arguments.nthreads);
		cxt.configure();
		output.closeBlock("compareFiles");
		
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
		} catch (QuickBuildException ex) {
			System.out.println(ex.getMessage());
			System.exit(1);
		}
	}
}
