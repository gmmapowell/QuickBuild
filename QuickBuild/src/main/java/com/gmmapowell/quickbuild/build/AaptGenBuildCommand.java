package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class AaptGenBuildCommand implements BuildCommand {

	private final Project project;
	private File gendir;
	private File manifestFile;
	private File resdir;
	private File platformJar;
	private File aapt;

	public AaptGenBuildCommand(Config config, Project project) {
		this.project = project;
		this.gendir = project.getDir("gen");
		this.manifestFile = project.getDir("AndroidManifest.xml");
		this.resdir = project.getDir("res");
		File platformRoot = config.getAndroidPlatformRoot();
		this.platformJar = new File(platformRoot, "android.jar");
		this.aapt = new File(platformRoot, "tools/aapt.exe");
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
		RunProcess proc = new RunProcess(aapt.getPath());
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
		proc.arg(platformJar.getPath());
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
		return "aapt gen: " + FileUtils.relativeTo(gendir);
	}
}
