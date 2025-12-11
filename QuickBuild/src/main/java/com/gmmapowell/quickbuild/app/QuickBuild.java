package com.gmmapowell.quickbuild.app;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

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
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.utils.ProcessArgs;
import com.gmmapowell.vc.VCHelper;

public class QuickBuild {
	private static ArgumentDefinition[] argumentDefinitions = new ArgumentDefinition[] {
		new ArgumentDefinition("*.qb", Cardinality.REQUIRED, "file", "configuration file"),
		new ArgumentDefinition("--alltests", Cardinality.OPTION, "allTests", "run all tests, including ones marked @QuickIgnore"),
		new ArgumentDefinition("--args", Cardinality.LIST, "showArgsFor", null),
		new ArgumentDefinition("--blank", Cardinality.OPTION, "blank", "blank memory"),
		new ArgumentDefinition("--build-all", Cardinality.OPTION, "buildAll", "build everything"),
		new ArgumentDefinition("--cache", Cardinality.OPTION, "cachedir", "Cache directory"),
		new ArgumentDefinition("--config-only", Cardinality.OPTION, "configOnly", "only run config; don't actually build"),
		new ArgumentDefinition("--debug", Cardinality.LIST, "showDebugFor", "show debug for targets"),
		new ArgumentDefinition("--debugInternals", Cardinality.LIST, "debug", "debug internals of QuickBuild"),
		new ArgumentDefinition("--why", Cardinality.LIST, "why", "explain why target will be built"),
		new ArgumentDefinition("--doublequick", Cardinality.OPTION, "doubleQuick", "avoid time-consuming non-critical-path items"),
		new ArgumentDefinition("--ignore-main-changes", Cardinality.OPTION, "ignoreMain", "don't blank memory if you changed something trivial in a main file"),
		new ArgumentDefinition("--nthreads", Cardinality.OPTION, "nthreads", "number of threads"),
		new ArgumentDefinition("--no-home", Cardinality.OPTION, "readHome", "don't read files in home directory"),
		new ArgumentDefinition("--quiet", Cardinality.LIST, "quiet", "super quiet mode"),
		new ArgumentDefinition("--testAlways", Cardinality.OPTION, "testAlways", "run all test cases, regardless if anything has changed"),
		new ArgumentDefinition("--no-check-git", Cardinality.OPTION, "checkGit", "Don't run git fetch"),
		new ArgumentDefinition("--grand-fallacy", Cardinality.OPTION, "gfMode", "invert grand fallacy mode"),
		new ArgumentDefinition("--teamcity", Cardinality.OPTION, "teamcity", "TeamCity integration mode"),
		new ArgumentDefinition("--upto", Cardinality.OPTION, "upTo", "last target to build")
	};

	private static Arguments arguments;

	private static ConfigFactory configFactory = new ConfigFactory();
	
	public static void setVCHelper(VCHelper helper) {
		configFactory.vchelper = helper;
	}
	
	public static void main(String[] args)
	{
		long start = new Date().getTime();
		try {
			List<File> pathElts = FileUtils.splitJavaPath(System.getProperty("java.class.path"));
			File utilsJar = null;
			for (File f : pathElts) {
				if (f.getPath().endsWith("/Utils.jar") ||
					f.getPath().endsWith("/Utils/bin/classes") ||
					f.getPath().endsWith("/Quickbuilder.jar") ||
					f.getPath().endsWith("/Utils/bin")) {
					utilsJar = f;
					break;
				}
			}
			if (utilsJar == null) {
				TreeSet<File> pe = new TreeSet<>(pathElts);
				for (File p : pe) {
					System.out.println(p);
				}
				throw new QuickBuildException("Could not find Utils.jar on the class path");
			}
			Date launched = new Date();
			arguments = new Arguments();
			ProcessArgs.process(arguments, argumentDefinitions, args);
			setVCHelper(new GitHelper(arguments.showTimings));
			BuildOutput output = new BuildOutput(arguments.teamcity);
			output.openBlock("Config");
	
			if (arguments.debug)
				System.out.println("user.home = " + System.getProperty("user.home"));
			File file = new File(arguments.file);
			OrderedFileList ofl = new OrderedFileList(FileUtils.relativePath(file));
			Config conf = new Config(configFactory, output, file.getParentFile(), FileUtils.dropExtension(file.getName()), arguments.cachedir);
			{
				File hostfile = new File(file.getParentFile(), FileUtils.getHostName() + ".host.qb");
				if (arguments.debug)
					System.out.println("Reading host file " + hostfile + " " + (hostfile.exists()?"*":"-"));
				if (hostfile.exists())
				{
					SignificantWhiteSpaceFileReader.read(conf, configFactory, hostfile);
					ofl.add(hostfile);
				}
			}
			if (arguments.readHome)
				readHomeConfig(conf, ofl);
			try {
				SignificantWhiteSpaceFileReader.read(conf, configFactory, file);
			} catch (Exception ex) {
				Throwable t = UtilException.unwrap(ex);
				System.out.println("ERROR parsing configuration");
				System.out.println(t.getMessage());
				return;
			}
			conf.done();
			configFactory.done();
			VCHelper helper = conf.helper;
	
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
			output.closeBlock("Config");
			output.openBlock("compareFiles");
			if (!arguments.quiet)
				output.println("Comparing files ...");
			if (arguments.showTimings) {
				long time1 = new Date().getTime();
				System.out.println("Timing #1: " + (time1-start));
			}
			List<String> notclean = helper.checkRepositoryClean(null, false);
			for (String s : notclean)
				System.out.println("WARNING: the directory " + s + " is not owned by git");
			if (arguments.checkGit) {
				if (arguments.showTimings) {
					long time1a = new Date().getTime();
					System.out.println("Timing #1a: " + (time1a-start));
				}
				List<String> missing = helper.checkMissingCommits();
				for (String s : missing)
					System.out.println("WARNING: Your repository is missing " + s);
			}
			if (arguments.showTimings) {
				long time2 = new Date().getTime();
				System.out.println("Timing #2: " + (time2-start));
			}
			GitRecord mainFiles = helper.checkFiles(true, ofl, new File(conf.getCacheDir(), file.getName()));
			if (!arguments.ignoreMain) {
				blankMemory |= mainFiles.isDirty();
				buildAll |= mainFiles.isDirty();
			}

			if (arguments.showTimings) {
				long time2b = new Date().getTime();
				System.out.println("Timing #2b: " + (time2b-start));
			}
			// now we need to read back anything we've cached ...
			BuildContext cxt = new BuildContext(conf, configFactory, output, blankMemory, buildAll, arguments.debug, arguments.showArgsFor, arguments.showDebugFor, arguments.quiet, utilsJar, arguments.upTo, arguments.doubleQuick, arguments.allTests, arguments.gfMode, arguments.testAlways, arguments.why, arguments.showTimings);
			if (arguments.showTimings) {
				long time2c = new Date().getTime();
				System.out.println("Timing #2c: " + (time2c-start));
			}
			cxt.configure();
			if (arguments.showTimings) {
				long time3 = new Date().getTime();
				System.out.println("Timing #3: " + (time3-start));
			}
			
			if (!arguments.quiet && !output.forTeamCity())
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
				System.out.println("Pre-build configuration time: " + DateUtils.elapsedTime(launched, new Date(), DateUtils.Format.hhmmss3));
			output.closeBlock("compareFiles");
	
			mainFiles.commit();
			cxt.getBuildOrder().commitUnbuilt();
			new BuildExecutor(cxt, arguments.debug).doBuild();
		} catch (QuickBuildException ex) {
			System.out.println(ex.getMessage());
			System.exit(1);
		}
	}

	public static void readHomeConfig(Config conf, OrderedFileList ofl) {
		{
			File roothostfile = new File(new File(System.getProperty("user.home")), ".qbinit." + FileUtils.getHostName());
			if (arguments != null && arguments.debug)
				System.out.println("Reading root host file " + roothostfile + " " + (roothostfile.exists()?"*":"-"));
			if (roothostfile.exists())
			{
				SignificantWhiteSpaceFileReader.read(conf, configFactory, roothostfile);
				if (ofl != null)
					ofl.add(roothostfile);
			}
		}
		{
			File rootfile = new File(new File(System.getProperty("user.home")), ".qbinit");
			if (arguments != null && arguments.debug)
				System.out.println("Reading root file " + rootfile + " " + (rootfile.exists()?"*":"-"));
			if (rootfile.exists())
			{
				SignificantWhiteSpaceFileReader.read(conf, configFactory, rootfile);
				if (ofl != null)
					ofl.add(rootfile);
			}
		}
	}
}
