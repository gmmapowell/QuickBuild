package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class ApkBuildCommand implements BuildCommand {

	private final Project project;
	private final AndroidContext acxt;
	private final File zipfile;
	private final File dexFile;
	private final File apkFile;

	public ApkBuildCommand(AndroidContext acxt, Project project, File zipfile, File dexFile, File apkFile) {
		this.acxt = acxt;
		this.project = project;
		this.zipfile = zipfile;
		this.dexFile = dexFile;
		this.apkFile = apkFile;
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
		RunProcess proc = new RunProcess(acxt.getAPKBuilder().getPath());
		proc.captureStdout();
		proc.captureStderr();
		
		proc.arg(apkFile.getPath());
		proc.arg("-d");
		proc.arg("-z");
		proc.arg(zipfile.getPath());
		proc.arg("-f");
		proc.arg(dexFile.getPath());
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
		return "apk builder: " + FileUtils.makeRelative(apkFile);
	}
}
