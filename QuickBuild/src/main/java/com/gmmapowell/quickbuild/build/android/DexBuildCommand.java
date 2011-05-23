package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class DexBuildCommand implements Tactic {
	private final AndroidContext acxt;
	private final File bindir;
	private final File dexFile;
	private final List<File> jars = new ArrayList<File>();
	private final File libdir;
	private final Strategem parent;

	public DexBuildCommand(AndroidContext acxt, Strategem parent, StructureHelper files, File bindir, File libdir, File dexFile) {
		this.acxt = acxt;
		this.parent = parent;
		this.bindir = bindir;
		this.libdir = libdir;
		this.dexFile = dexFile;
	}

	public void addJar(File file) {
		jars.add(file);
		
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
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

	@Override
	public Strategem belongsTo() {
		return parent;
	}


	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "dex");
	}


}
