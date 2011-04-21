package com.gmmapowell.quickbuild.build;

import java.io.File;
import com.gmmapowell.quickbuild.config.AndroidContext;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class ApkBuildCommand implements Tactic {

	private final AndroidContext acxt;
	private final File zipfile;
	private final File dexFile;
	private final File apkFile;
	ApkResource apkResource;

	public ApkBuildCommand(AndroidContext acxt, File zipfile, File dexFile, File apkFile) {
		this.acxt = acxt;
		this.zipfile = zipfile;
		this.dexFile = dexFile;
		this.apkFile = apkFile;
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
			cxt.addBuiltResource(apkResource);
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
		// TODO Auto-generated method stub
		return null;
	}
}
