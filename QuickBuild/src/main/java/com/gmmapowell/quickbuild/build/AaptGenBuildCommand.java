package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class AaptGenBuildCommand implements BuildCommand {

	private final Project project;
	private final AndroidContext acxt;
	private final File gendir;
	private final File manifestFile;
	private final File resdir;

	public AaptGenBuildCommand(AndroidContext acxt, Project project, File manifest, File gendir, File resdir) {
		this.acxt = acxt;
		this.project = project;
		this.gendir = gendir;
		this.manifestFile = manifest;
		this.resdir = resdir;
	}
	
	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public List<BuildResource> generatedResources() {
		return null;
	}

	@Override
	public Set<String> getPackagesProvided() {
		return null;
	}

	@Override
	public boolean execute(BuildContext cxt) {
		FileUtils.cleanDirectory(gendir);
		RunProcess proc = new RunProcess(acxt.getAAPT().getPath());
//		proc.showArgs(true);
		proc.captureStdout();
		proc.captureStderr();
		
		proc.arg("package");
		proc.arg("-m");
		proc.arg("-J");
		proc.arg(gendir.getPath());
		proc.arg("-M");
		proc.arg(manifestFile.getPath());
		proc.arg("-S");
		proc.arg(resdir.getPath());
		proc.arg("-I");
		proc.arg(acxt.getPlatformJar().getPath());
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			return true; // success
		}
		System.out.println(proc.getStderr());
		throw new QuickBuildException("The exit code was " + proc.getExitCode());
	}

	@Override
	public String toString() {
		return "aapt gen: " + FileUtils.makeRelative(gendir);
	}
}
