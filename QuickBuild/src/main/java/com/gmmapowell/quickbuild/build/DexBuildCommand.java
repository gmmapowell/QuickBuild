package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class DexBuildCommand implements BuildCommand {
	private final AndroidContext acxt;
	private final Project project;
	private final File bindir;
	private final File dexFile;
	private final List<File> jars = new ArrayList<File>();
	private final File libdir;

	public DexBuildCommand(AndroidContext acxt, Project project, File bindir, File dexFile) {
		this.acxt = acxt;
		this.project = project;
		this.bindir = bindir;
		this.libdir = project.getRelative("lib");
		this.dexFile = dexFile;
	}

	@Override
	public Project getProject() {
		return project;
	}
	
	@Override
	public List<BuildResource> generatedResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getPackagesProvided() {
		// TODO Auto-generated method stub
		return null;
	}

	public void addJar(File file) {
		jars.add(file);
		
	}

	@Override
	public BuildStatus execute(BuildContext cxt) {
		RunProcess proc = new RunProcess(acxt.getDX().getPath());
		proc.captureStdout();
		proc.captureStderr();
		proc.showArgs(true);
		
		proc.arg("--dex");
		proc.arg("--output="+dexFile.getPath());
		proc.arg(bindir.getPath());
		
		for (File f : FileUtils.findFilesMatching(libdir, "*.jar"))
			proc.arg(f.getPath());
		for (File f : jars)
			proc.arg(f.getPath());

		proc.execute();
		if (proc.getStderr().length() > 0 || proc.getStdout().length() > 0)
		{
			System.out.println(proc.getStdout());
			System.out.println(proc.getStderr());
			return BuildStatus.BROKEN;
		}
		if (proc.getExitCode() == 0)
		{
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}
	
	@Override
	public String toString() {
		return "Create Dex: " + dexFile;
	}



}
