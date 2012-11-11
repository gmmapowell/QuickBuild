package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JarBuildCommand implements Tactic {
	private final Strategem parent;
	private final File jarfile;
	private final JarResource jarResource;
	private final List<File> dirsToJar = new ArrayList<File>();
	private final List<File> includePackages;
	private final List<File> excludePackages;

	public JarBuildCommand(Strategem parent, StructureHelper files, String targetName, List<File> includePackages, List<File> excludePackages) {
		this.parent = parent;
		this.jarResource = new JarResource(this, files.getOutput(FileUtils.ensureExtension(targetName, ".jar")));;
		this.includePackages = includePackages;
		this.excludePackages = excludePackages;
		this.jarfile = jarResource.getPath();
	}
	
	public void add(File file) {
		dirsToJar.add(file);
	}
	
	public File getFile() {
		return FileUtils.makeRelative(jarfile);
	}

	public JarResource getJarResource() {
		return jarResource;
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (jarfile.exists() && !jarfile.delete())
			throw new QuickBuildException("Could not delete " + jarfile);
		RunProcess proc = new RunProcess("jar");
		if (showArgs)
			proc.showArgs(showArgs);
		proc.captureStdout();
		proc.redirectStderr(System.out);
		proc.arg("cvf");
		proc.arg(jarfile.getPath());
		boolean hasFiles = hasFiles(proc);
		if (!hasFiles)
		{
			// we didn't actually build it, but it wants reassurance ...
			try
			{
				if (jarResource != null)
					jarResource.getFile().createNewFile();
				cxt.builtResource(jarResource, false);
			}
			catch (Exception ex)
			{
				throw UtilException.wrap(ex);
			}
			return BuildStatus.SKIPPED;
		}
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			cxt.builtResource(jarResource);
			return BuildStatus.SUCCESS;
		}
		return BuildStatus.BROKEN;
	}

	boolean hasFiles(RunProcess proc) {
		boolean hasFiles = false;
		for (File dir : dirsToJar)
		{
			for (File f : FileUtils.findFilesUnderMatching(dir, "*"))
			{
				if (new File(dir, f.getPath()).isDirectory())
					continue;
				if (blockedByFilters(f))
					continue;
				if (f.getName().startsWith(".git"))
					continue;
				if (proc != null)
				{
					proc.arg("-C");
					proc.arg(dir.getPath());
					proc.arg(f.getPath());
				}
				hasFiles = true;
			}
		}
		return hasFiles;
	}

	private boolean blockedByFilters(File f) {
		if (includePackages != null)
		{
			for (File u : includePackages)
				if (FileUtils.isUnder(f, u))
					return false;
			return true;
		}
		if (excludePackages != null)
		{
			for (File u : excludePackages)
				if (FileUtils.isUnder(f, u))
					return true;
			return false;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Jar Up: " + jarResource;
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "jar");
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
