package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
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
	private final Set<Pattern> exclusions;
	private ResourcePacket<PendingResource> uselibs;

	public DexBuildCommand(AndroidContext acxt, Strategem parent, StructureHelper files, File bindir, File libdir, File dexFile, Set<Pattern> exclusions, ResourcePacket<PendingResource> uselibs) {
		this.acxt = acxt;
		this.parent = parent;
		this.bindir = bindir;
		this.libdir = libdir;
		this.dexFile = dexFile;
		this.exclusions = exclusions;
		this.uselibs = uselibs;
	}

	public void addJar(File file) {
		jars.add(file);
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		RunProcess proc = new RunProcess(acxt.getDX().getPath());
		proc.captureStdout();
		proc.captureStderr();
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		
		if (uselibs != null)
		{
			for (PendingResource pr : uselibs)
			{
				File path = pr.physicalResource().getPath();
				addJar(path);
			}
			uselibs = null;
		}
		
		proc.arg("--dex");
		proc.arg("--output="+dexFile.getPath());
		proc.arg(bindir.getPath());
		
		LinkedHashSet<String> paths = new LinkedHashSet<String>();
		for (BuildResource br : cxt.getDependencies(this))
		{
			if (br instanceof JarResource)
				considerAdding(paths, br.getPath().getPath());
		}
		
		for (File f : FileUtils.findFilesMatching(libdir, "*.jar"))
			considerAdding(paths, f.getPath());
		for (File f : jars)
			considerAdding(paths, f.getPath());

		for (String s : paths)
			proc.arg(s);
		
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
	
	private void considerAdding(LinkedHashSet<String> paths, String path) {
		for (Pattern patt : exclusions)
			if (patt.matcher(path.toLowerCase().replaceAll("\\\\", "/")).matches())
				return;
		paths.add(path);
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

	private Set <Tactic> procDeps = new HashSet<Tactic>();
	
	@Override
	public void addProcessDependency(Tactic earlier) {
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}
}
