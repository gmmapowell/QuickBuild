package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.BuildResource;
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
	private boolean showArgs;
	private boolean doClean = true;
	private final Strategem parent;
	private List<File> sources;

	public JavaBuildCommand(Strategem parent, StructureHelper files, String src, String bin, List<File> sources) {
		this.parent = parent;
		this.sources = sources;
		this.srcdir = new File(files.getBaseDir(), src);
		this.bindir = new File(files.getOutputDir(), bin);
		if (!bindir.exists())
			if (!bindir.mkdirs())
				throw new QuickBuildException("Cannot build " + srcdir + " because the build directory cannot be created");
		if (bindir.exists() && !bindir.isDirectory())
			throw new QuickBuildException("Cannot build " + srcdir + " because the build directory is not a directory");
		this.classpath = new BuildClassPath();
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
	public BuildStatus execute(BuildContext cxt) {
		JavaNature nature = cxt.getNature(JavaNature.class);
		if (nature == null)
			throw new UtilException("There is no JavaNature installed (huh?)");
		if (!srcdir.isDirectory())
			return BuildStatus.SKIPPED;
		if (doClean)
			FileUtils.cleanDirectory(bindir);
		classpath.add(bindir);
		for (BuildResource br : cxt.getDependencies(parent))
		{
			if (br instanceof JarResource)
				classpath.add(((JarResource)br).getPath());
		}
		RunProcess proc = new RunProcess("javac");
		proc.showArgs(showArgs);
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
		}
		if (proc.getExitCode() == 1)
		{
			// compilation errors, usually
			LinePatternParser lpp = new LinePatternParser();
			lpp.match("package ([a-zA-Z0-9_.]*) does not exist", "nopackage", "pkgname");
			int cnt = 0;
			for (LinePatternMatch lpm : lpp.applyTo(new StringReader(proc.getStderr())))
			{
				if (lpm.is("nopackage"))
				{
					if (nature.addDependency(parent, lpm.get("pkgname")))
						cnt++;
				}
				else
					throw new QuickBuildException("Do not know how to handle match " + lpm);
			}
			if (cnt > 0)
				return BuildStatus.RETRY;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		return "Java Compile: " + srcdir;
	}

	public void showArgs(boolean b) {
		showArgs = b;
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}
}
