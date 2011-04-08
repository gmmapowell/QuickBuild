package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class AaptGenBuildCommand implements BuildCommand {

	private final Project project;
	private final AndroidContext acxt;
	private final File gendir;
	private final File manifestFile;
	private final File resdir;
	private DirectoryResource resResource;

	public AaptGenBuildCommand(AndroidContext acxt, Project project, File manifest, File gendir, File resdir) {
		this.acxt = acxt;
		this.project = project;
		this.gendir = gendir;
		this.manifestFile = manifest;
		this.resdir = resdir;
		resResource = new DirectoryResource(null, resdir);
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
	public BuildStatus execute(BuildContext cxt) {
		if (!cxt.requiresBuiltResource(this, resResource))
		{
			System.out.println("Need resource '" + resResource + "' ... failing");
			return BuildStatus.RETRY;
		}
		FileUtils.assertDirectory(gendir);
		FileUtils.cleanDirectory(gendir);
		RunProcess proc = new RunProcess(acxt.getAAPT().getPath());
		proc.showArgs(true);
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
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		return "aapt gen: " + FileUtils.makeRelative(gendir);
	}
}
