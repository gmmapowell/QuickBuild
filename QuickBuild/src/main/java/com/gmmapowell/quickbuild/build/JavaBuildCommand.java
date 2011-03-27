package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.io.StringReader;

import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JavaBuildCommand implements BuildCommand {
	private final Project project;
	private final File srcdir;
	private final File bindir;
	private final BuildClassPath classpath;

	public JavaBuildCommand(Project project, String src, String bin) {
		this.project = project;
		this.srcdir = new File(project.getBaseDir(), src);
		this.bindir = new File(project.getOutputDir(), bin);
		if (!bindir.exists())
			if (!bindir.mkdirs())
				throw new QuickBuildException("Cannot build " + srcdir + " because the build directory cannot be created");
		if (bindir.exists() && !bindir.isDirectory())
			throw new QuickBuildException("Cannot build " + srcdir + " because the build directory is not a directory");
		this.classpath = new BuildClassPath();
	}
	
	public void addJar(File file) {
		classpath.add(file);
	}

	@Override
	public boolean execute(BuildContext cxt) {
		RunProcess proc = new RunProcess("javac.exe");
		proc.captureStdout();
		proc.captureStderr();
		
		proc.arg("-sourcepath");
		proc.arg(srcdir.getPath());
		proc.arg("-d");
		proc.arg(bindir.getPath());
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		for (File f : FileUtils.findFilesMatching(srcdir, "*.java"))
		{
			proc.arg(f.getPath());
		}
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			cxt.addClassDirForProject(project, bindir);
			return true; // success
		}
		if (proc.getExitCode() == 1)
		{
			// compilation errors, usually
			LinePatternParser lpp = new LinePatternParser();
			lpp.match("package ([a-zA-Z0-9_.]*) does not exist", "nopackage", "pkgname");
			for (LinePatternMatch lpm : lpp.applyTo(new StringReader(proc.getStderr())))
			{
				if (lpm.is("nopackage"))
				{
					cxt.addDependency(this, lpm.get("pkgname"));
				}
				else
					throw new QuickBuildException("Do not know how to handle match " + lpm);
			}
			return false;
		}
		System.out.println(proc.getStderr());
		throw new QuickBuildException("The exit code was " + proc.getExitCode());
	}

	@Override
	public String toString() {
		return "Java Compile: " + srcdir;
	}

	public Project getProject() {
		return project;
	}
}
