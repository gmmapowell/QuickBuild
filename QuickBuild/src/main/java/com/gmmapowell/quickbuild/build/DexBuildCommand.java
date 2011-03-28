package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.system.RunProcess;

public class DexBuildCommand implements BuildCommand {
	private final AndroidContext acxt;
	private final Project project;
	private final File bindir;
	private final File dexFile;

	public DexBuildCommand(AndroidContext acxt, Project project, File bindir, File dexFile) {
		this.acxt = acxt;
		this.project = project;
		this.bindir = bindir;
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

	@Override
	public BuildStatus execute(BuildContext cxt) {
		RunProcess proc = new RunProcess(acxt.getDX().getPath());
		proc.showArgs(true);
		proc.captureStdout();
		proc.captureStderr();
		
		proc.arg("--dex");
		proc.arg("--output="+dexFile.getPath());
		proc.arg(bindir.getPath());
		if (proc.getExitCode() == 0)
		{
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;	}
	
}
