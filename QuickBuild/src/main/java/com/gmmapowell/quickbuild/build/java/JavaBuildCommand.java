package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JavaBuildCommand implements Tactic {
	private final File srcdir;
	private final File bindir;
	private final BuildClassPath classpath;
	private final BuildClassPath bootclasspath;
	private boolean doClean = true;
	private final Strategem parent;
	private List<File> sources;
	private final String label;
	private final String context;

	public JavaBuildCommand(Strategem parent, StructureHelper files, String src, String bin, String label, List<File> sources, String context) {
		this.parent = parent;
		this.label = label;
		this.sources = sources;
		this.context = context;
		this.srcdir = new File(files.getBaseDir(), src);
		this.bindir = new File(files.getOutputDir(), bin);
		if (!bindir.exists())
			if (!bindir.mkdirs())
				throw new QuickBuildException("Cannot build " + srcdir + " because the build directory cannot be created");
		if (bindir.exists() && !bindir.isDirectory())
			throw new QuickBuildException("Cannot build " + srcdir + " because the build directory is not a directory");
		this.classpath = new BuildClassPath();
		this.classpath.add(bindir);
		this.bootclasspath = new BuildClassPath();
	}
	
	public void addToClasspath(File file) {
		classpath.add(FileUtils.relativePath(file));
	}

	public void addToBootClasspath(File file) {
		bootclasspath.add(FileUtils.relativePath(file));
	}
	
	public BuildClassPath getClassPath() {
		return classpath;
	}

	public void dontClean()
	{
		doClean = false;
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		JavaNature nature = cxt.getNature(JavaNature.class);
		if (nature == null)
			throw new UtilException("There is no JavaNature installed (huh?)");
		if (!srcdir.isDirectory())
			return BuildStatus.SKIPPED;
		if (doClean)
			FileUtils.cleanDirectory(bindir);
		classpath.add(bindir);
		for (BuildResource br : cxt.getDependencies(this))
		{
			if (br instanceof JarResource)
				classpath.add(((JarResource)br).getPath());
			else if (br instanceof ProcessResource)
				; // transitive node
			else
				System.out.println("What do I do with " + br);
		}
		RunProcess proc = new RunProcess("javac");
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();
	
		if (!bootclasspath.empty())
		{
			proc.arg("-bootclasspath");
			proc.arg(bootclasspath.toString());
		}
		proc.arg("-sourcepath");
		proc.arg(srcdir.getPath());
		proc.arg("-d");
		proc.arg(bindir.getPath());
		if (!classpath.empty())
		{
			proc.arg("-classpath");
			proc.arg(classpath.toString());
		}
		boolean any = false;
		for (File f : sources)
		{
			proc.arg(f.getPath());
			any = true;
		}
		if (!any)
			return BuildStatus.SKIPPED;
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			// TODO: cxt.addClassDirForProject(project, bindir);
			return BuildStatus.SUCCESS;
		} else if (cxt.grandFallacy) {
			System.out.println("Grand Fallacy mode ... not bothering to fix errors");
			return BuildStatus.BROKEN;
		}
		// compilation errors, usually
		List<String> mypackages = new ArrayList<String>();
		LinePatternParser lpp = new LinePatternParser();
		lpp.match("package ([a-zA-Z0-9_.]*) does not exist", "nopackage", "pkgname");
		lpp.match("cannot access ([a-zA-Z0-9_.]*)\\.[a-zA-Z0-9_]*", "nopackage", "pkgname");
		lpp.match("location: class ([a-zA-Z0-9_.]*)\\.[a-zA-Z0-9_]*", "location", "mypackage");
		int cnt = 0;
		for (LinePatternMatch lpm : lpp.applyTo(new StringReader(proc.getStderr())))
		{
			if (lpm.is("nopackage"))
			{
				String pkg = lpm.get("pkgname");
				if (showDebug)
					System.out.println("Looking for " + pkg);
				if (nature.addDependency(this, pkg, context, showDebug))
				{
					if (showDebug)
						System.out.println("  ... added for package " + pkg);
					cnt++;
				}
			}
			else if (lpm.is("location"))
				mypackages.add(lpm.get("mypackage"));
			else
				throw new QuickBuildException("Do not know how to handle match " + lpm);
		}
		if (cnt > 0)
		{
			System.out.println("       Corrected errors by adding " + cnt + " dependencies");
			return BuildStatus.RETRY;
		}
		// There is an element of desperation here, but what can you do?
		// See if we can find other jars that produce the same package as we are currently compiling
		for (String pkg : mypackages)
		{
			if (showDebug)
				System.out.println("Trying to find other implementations of " + pkg);
			if (nature.addDependency(this, pkg, context, showDebug))
			{
				if (showDebug)
					System.out.println("  ... added for package " + pkg);
				cnt++;
			}
		}
		if (cnt > 0)
		{
			System.out.println("       Corrected errors by adding " + cnt + " files with similar packages");
			return BuildStatus.RETRY;
		}
		System.out.println("!!! Errors were detected in javac, but could not be corrected:");
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		return "Java Compile: " + srcdir;
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, label);
	}

	private Set <Tactic> procDeps = new HashSet<Tactic>();
	
	@Override
	public void addProcessDependency(Tactic earlier) {
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}
}
