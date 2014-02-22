package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.CanBeSkipped;
import com.gmmapowell.quickbuild.build.ErrorCase;
import com.gmmapowell.quickbuild.config.AbstractBuildCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.FloatToEnd;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class JavaDocCommand extends AbstractBuildCommand implements FloatToEnd, CanBeSkipped {
	private String overview;
	private final File rootdir;
	private String outdir;
	private File outputdir;
	private List<String> projects = new ArrayList<String>();
	private BuildClassPath bootclasspath;
	private ResourcePacket<PendingResource> dependsOn = new ResourcePacket<PendingResource>();

	@SuppressWarnings("unchecked")
	public JavaDocCommand(TokenizedLine toks) {
		toks.process(this,
				new ArgumentDefinition("*", Cardinality.OPTION, "outdir", "output directory"));
		this.rootdir = FileUtils.getCurrentDir();
		this.bootclasspath = new BuildClassPath();
	}

	public void addToBootClasspath(File file) {
		bootclasspath.add(FileUtils.relativePath(file));
	}
	
	@Override
	public Strategem applyConfig(Config config) {
		outputdir = new File(rootdir, outdir);
		super.handleOptions(config);
		return this;
	}

	@Override
	public boolean handleOption(Config config, ConfigApplyCommand opt)
	{
		if (super.handleOption(config, opt))
			return true;
		else if (opt instanceof IncludePackageCommand) {
			String proj = ((IncludePackageCommand) opt).getPackage();
			projects.add(proj);
			dependsOn.add(new PendingResource(proj+"/qbout/"+proj+".jar"));
		} else if (opt instanceof OverviewCommand)
			overview = ((OverviewCommand) opt).overview;
		else if (opt instanceof BootClassPathCommand)
			addToBootClasspath(((BootClassPathCommand)opt).getFile());
		else
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "Javadoc " + outdir;
	}

	@Override
	public boolean skipMe(BuildContext cxt) {
		return cxt.doubleQuick;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		JavaNature nature = cxt.getNature(JavaNature.class);
		if (nature == null)
			throw new UtilException("There is no JavaNature installed (huh?)");
		
		BuildClassPath classpath = new BuildClassPath();
		for (BuildResource br : cxt.getResources(JarResource.class))
		{
			File path = ((JarResource)br).getPath();
			classpath.add(path);
			cxt.addDependency(this, br, showDebug);
		}
		
		BuildClassPath sourcepath = new BuildClassPath();
		Set<String> packageNames = new TreeSet<String>();
		for (String s : projects)
		{
			File path = FileUtils.combine(new File(s), "src/main/java");
			sourcepath.add(path);
			for (File f : FileUtils.findFilesUnderMatching(path, "*.java"))
				packageNames.add(FileUtils.convertToDottedName(f.getParentFile()));
		}
		if (sourcepath.empty())
			return BuildStatus.SKIPPED;
		if (cxt.doubleQuick)
			return BuildStatus.SKIPPED;

		FileUtils.assertDirectory(outputdir);
		RunProcess proc = new RunProcess("javadoc");
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();
	
		if (!bootclasspath.empty())
		{
			proc.arg("-bootclasspath");
			proc.arg(bootclasspath.toString());
		}
		proc.arg("-d");
		proc.arg(outputdir.getPath());
		proc.arg("-sourcepath");
		proc.arg(sourcepath.toString());
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		if (overview != null)
		{
			proc.arg("-overview");
			proc.arg(overview);
		}
		boolean any = false;
		for (String f : packageNames)
		{
			proc.arg(f);
			any = true;
		}
		if (!any)
			return BuildStatus.SKIPPED;
		proc.execute();
		
		ErrorCase failure = null;
		LinePatternParser lppOut = new LinePatternParser();
		lppOut.match("([0-9][0-9]*) warnings", "warnings", "count");
		int cnt = 0;
		for (LinePatternMatch lpm : lppOut.applyTo(new StringReader(proc.getStdout())))
		{
			if (lpm.is("warnings"))
			{
				if (failure == null)
					failure = cxt.failure(proc.getArgs(), proc.getStdout(), proc.getStderr());
				failure.addMessage("JavaDoc encountered " + lpm.get("count") + " warnings:");
			}
			else
				throw new QuickBuildException("Do not know how to handle match " + lpm);
		}

		LinePatternParser lppErr = new LinePatternParser();
		lppErr.match("src/[^/]+/java/(.*): warning - (.*)", "message", "location", "text");
		lppErr.match("(javadoc: error.*)", "error", "text");
		lppErr.match("(error: .*)", "error", "text");
		for (LinePatternMatch lpm : lppErr.applyTo(new StringReader(proc.getStderr())))
		{
			if (lpm.is("message"))
			{
				if (failure == null)
					failure = cxt.failure(proc.getArgs(), proc.getStdout(), proc.getStderr());
				failure.addMessage("  " + lpm.get("location") + ": " + lpm.get("text"));
				cnt++;
			}
			else if (lpm.is("error"))
			{
				if (failure == null)
					failure = cxt.failure(proc.getArgs(), proc.getStdout(), proc.getStderr());
				failure.addMessage("  " + lpm.get("text"));
				cnt++;
			}
			else
				throw new QuickBuildException("Do not know how to handle match " + lpm);
		}
		
		if (proc.getExitCode() == 0)
		{
			if (cnt == 0)
				return BuildStatus.SUCCESS;
			return BuildStatus.TEST_FAILURES;
		}
		if (failure == null)
			failure = cxt.failure(proc.getArgs(), proc.getStdout(), proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String identifier() {
		return "JavaDoc[" + outdir + "]";
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return dependsOn;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return new ResourcePacket<BuildResource>();
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		// maybe javadoc?
		return new ResourcePacket<BuildResource>();
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList();
	}

	@Override
	public boolean onCascade()
	{
		return true;
	}


	@Override
	public int priority() {
		return 5;
	}

	@Override
	public boolean analyzeExports() {
		return true;
	}
}
