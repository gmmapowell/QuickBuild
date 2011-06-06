package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class AaptGenBuildCommand implements Tactic {

	private final AndroidContext acxt;
	private final File gendir;
	private final File manifestFile;
	private final File resdir;
	private final AndroidCommand parent;

	public AaptGenBuildCommand(AndroidCommand parent, AndroidContext acxt, File manifest, File gendir, File resdir) {
		this.parent = parent;
		this.acxt = acxt;
		this.gendir = gendir;
		this.manifestFile = manifest;
		this.resdir = resdir;
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		FileUtils.assertDirectory(gendir);
		FileUtils.cleanDirectory(gendir);
		RunProcess proc = new RunProcess(acxt.getAAPT().getPath());
		proc.showArgs(showArgs);
		proc.debug(showDebug);
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

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "aaptgen");
	}
}
