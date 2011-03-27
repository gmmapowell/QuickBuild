package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.io.StringReader;

import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JavaBuildCommand implements BuildCommand {
	private final File projectDir;
	private final String src;
	private final String bin;
	private final File srcdir;
	private final BuildClassPath classpath;

	public JavaBuildCommand(File projectDir, String src, String bin) {
		this.projectDir = projectDir;
		this.src = src;
		this.bin = bin;
		this.srcdir = new File(projectDir, src);
		this.classpath = new BuildClassPath();
	}

	@Override
	public boolean execute(BuildContext cxt) {
		RunProcess proc = new RunProcess("javac.exe");
		proc.captureStdout();
		proc.captureStderr();
		
		proc.arg("-sourcepath");
		proc.arg(srcdir.getPath());
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		for (File f : FileUtils.findFilesMatching(srcdir, "*.java"))
		{
			proc.arg(f.getPath());
		}
		proc.execute();
		if (proc.getExitCode() == 0)
			return true; // success
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
		throw new QuickBuildException("The exit code was " + proc.getExitCode());
	}

	@Override
	public String toString() {
		return "Java Compile: " + src;
	}
}
