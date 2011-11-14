package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.ErrorCase;
import com.gmmapowell.quickbuild.build.android.AndroidJarCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.FloatToEnd;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class JavaDocCommand extends NoChildCommand implements ConfigBuildCommand, Strategem, Tactic, FloatToEnd {

	private String overview;
	private final File rootdir;
	private File outputdir;

	public JavaDocCommand(TokenizedLine toks) {
		toks.process(this,
				new ArgumentDefinition("*.html", Cardinality.OPTION, "overview", "overview"));
		this.rootdir = FileUtils.getCurrentDir();
	}


	@Override
	public Strategem applyConfig(Config config) {
		outputdir = new File(rootdir, "javadoc");
		return this;
	}

	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public String toString() {
		return "Javadoc";
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
			cxt.addDependency(this, br);
		}
		
		BuildClassPath sourcepath = new BuildClassPath();
		Set<String> packages = new TreeSet<String>();
		for (BuildResource br : cxt.getResources(JavaSourceDirResource.class))
		{
			if (br.getBuiltBy() instanceof AndroidJarCommand)
				continue;
			File path = ((JavaSourceDirResource)br).getPath();
			sourcepath.add(path);
			for (File f : FileUtils.findFilesUnderMatching(path, "*.java"))
				packages.add(FileUtils.convertToDottedName(f.getParentFile()));
			cxt.addDependency(this, br);
		}
		if (sourcepath.empty())
			return BuildStatus.SKIPPED;

		RunProcess proc = new RunProcess("javadoc");
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();
	
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
		for (String f : packages)
		{
			proc.arg(f);
			any = true;
		}
		if (!any)
			return BuildStatus.SKIPPED;
		proc.execute();
		
		ErrorCase failure = null;
		LinePatternParser lppOut = new LinePatternParser();
		lppOut.match("([0-9]+) warnings", "warnings", "count");
		int cnt = 0;
		for (LinePatternMatch lpm : lppOut.applyTo(new StringReader(proc.getStdout())))
		{
			if (lpm.is("warnings"))
			{
				if (failure == null)
					failure = cxt.failure(proc.getArgs(), proc.getStdout(), proc.getStderr());
				failure.addMessage("JavaDoc encountered " + lpm.get("count") + " warnings:");
				cnt++;
			}
			else
				throw new QuickBuildException("Do not know how to handle match " + lpm);
		}

		LinePatternParser lppErr = new LinePatternParser();
		lppErr.match("src/[^/]+/java/(.+): warning - (.*)", "message", "location", "text");
		for (LinePatternMatch lpm : lppErr.applyTo(new StringReader(proc.getStderr())))
		{
			if (lpm.is("message"))
			{
				if (failure == null)
					failure = cxt.failure(proc.getArgs(), proc.getStdout(), proc.getStderr());
				failure.addMessage("  " + lpm.get("location") + ": " + lpm.get("text"));
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
		return "JavaDoc[]";
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return new ResourcePacket<PendingResource>();
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
	public List<? extends Tactic> tactics() {
		return CollectionUtils.listOf(this);
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
