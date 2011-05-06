package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JarBuildCommand implements Tactic {
	private final Strategem parent;
	private final File jarfile;
	private final JarResource jar;
	private final List<File> dirsToJar = new ArrayList<File>();

	public JarBuildCommand(Strategem parent, StructureHelper files, String targetName) {
		this.parent = parent;
		this.jarfile = new File(files.getOutputDir(), targetName);
		jar = new JarResource(parent, this.jarfile);
	}
	
	public void add(File file) {
		dirsToJar.add(file);
	}
	
	public File getFile() {
		return FileUtils.makeRelative(jarfile);
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (jarfile.exists() && !jarfile.delete())
			throw new QuickBuildException("Could not delete " + jarfile);
		RunProcess proc = new RunProcess("jar");
		if (showArgs)
			proc.showArgs(showArgs);
		proc.captureStdout();
		proc.redirectStderr(System.out);
		proc.arg("cvf");
		proc.arg(jarfile.getPath());
		boolean hasFiles = false;
		for (File dir : dirsToJar)
		{
			for (File f : FileUtils.findFilesUnderMatching(dir, "*"))
			{
				if (new File(dir, f.getPath()).isDirectory())
					continue;
				proc.arg("-C");
				proc.arg(dir.getPath());
				proc.arg(f.getPath());
				hasFiles = true;
			}
		}
		if (!hasFiles)
			return BuildStatus.SKIPPED;
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			cxt.resourceAvailable(jar);
			return BuildStatus.SUCCESS;
		}
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		return "Jar Up: " + jar;
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}
}
