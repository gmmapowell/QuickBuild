package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class AaptPackageBuildCommand implements BuildCommand {

	private final Project project;
	private final AndroidContext acxt;
	private final File zipfile;
	private final File manifestFile;
	private final File resdir;
	private final File assetsDir;

	public AaptPackageBuildCommand(AndroidContext acxt, Project project, File manifest, File zipfile, File resdir, File assetsDir) {
		this.acxt = acxt;
		this.project = project;
		this.zipfile = zipfile;
		this.manifestFile = manifest;
		this.resdir = resdir;
		this.assetsDir = assetsDir;
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
		RunProcess proc = new RunProcess(acxt.getAAPT().getPath());
		proc.captureStdout();
		proc.captureStderr();
		
		proc.arg("package");
		proc.arg("-f");
		proc.arg("-F");
		proc.arg(zipfile.getPath());
		proc.arg("-M");
		proc.arg(manifestFile.getPath());
		if (assetsDir.exists())
		{
			proc.arg("-A");
			proc.arg(assetsDir.getPath());
		}
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
		return "aapt package: " + FileUtils.makeRelative(zipfile);
	}
}
