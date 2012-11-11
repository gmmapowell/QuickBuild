package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class ApkBuildCommand implements Tactic {

	private final AndroidContext acxt;
	private final File zipfile;
	private final File dexFile;
	private final File apkFile;
	private final ApkResource apkResource;
	private final AndroidCommand parent;

	public ApkBuildCommand(AndroidCommand parent, AndroidContext acxt, File zipfile, File dexFile, File apkFile, ApkResource apkResource) {
		this.parent = parent;
		this.acxt = acxt;
		this.zipfile = zipfile;
		this.dexFile = dexFile;
		this.apkFile = apkFile;
		this.apkResource = new ApkResource(this, apkFile);
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		RunProcess proc = new RunProcess(acxt.getAPKBuilder().getPath());
		proc.showArgs(showArgs);
		proc.debug(showDebug);
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
			cxt.builtResource(apkResource);
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		return "apk builder: " + FileUtils.makeRelative(apkFile);
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "apk");
	}

	public ApkResource getResource() {
		return apkResource;
	}

	private Set <Tactic> procDeps = new HashSet<Tactic>();
	
	@Override
	public void addProcessDependency(Tactic earlier) {
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}
}
