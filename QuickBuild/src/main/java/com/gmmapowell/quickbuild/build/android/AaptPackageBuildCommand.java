package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;

public class AaptPackageBuildCommand extends AbstractTactic {

	private final AndroidContext acxt;
	private final File zipfile;
	private final File manifestFile;
	private final File resdir;
	private final File assetsDir;
	private final File rawDir;

	public AaptPackageBuildCommand(AndroidCommand parent, AndroidContext acxt, File manifest, File zipfile, File resdir, File assetsDir, File rawDir) {
		super(parent);
		this.acxt = acxt;
		this.zipfile = zipfile;
		this.manifestFile = manifest;
		this.resdir = resdir;
		this.assetsDir = assetsDir;
		this.rawDir = rawDir;
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		RunProcess proc = new RunProcess(acxt.getAAPT().getPath());
		proc.debug(showDebug);
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
		if (rawDir != null && rawDir.exists()) {
			proc.arg(rawDir.getPath());
		}
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

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "aaptpkg");
	}
}
