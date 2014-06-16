package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.List;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;

public class PDEAssembleCommand extends ArchiveCommand {
	private final List<File> pdelibs;
	private final StructureHelper files;
	
	public PDEAssembleCommand(Strategem parent, StructureHelper files, String targetName, List<File> pdelibs, List<File> includePackages, List<File> excludePackages, OrderedFileList ofl) {
		super(parent, includePackages, excludePackages, ofl);
		this.files = files;
		this.pdelibs = pdelibs;
		if (files.getRelative("libs").isDirectory())
			this.pdelibs.add(files.getRelative("libs"));
		this.jarResource = new JarResource(this, files.getOutput(FileUtils.ensureExtension(targetName, ".jar")));
		this.jarfile = this.jarResource.getFile();
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
		proc.arg("cvmf");
		proc.arg(files.getRelative("META-INF/MANIFEST.MF").getPath());
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
				hasFiles |= jarFile(proc, dir, f);
			}
		}
		for (File dir : pdelibs)
		{
			for (File f : FileUtils.findFilesUnderMatching(dir, "*"))
			{
				hasFiles |= jarFile(proc, dir.getParentFile(), FileUtils.combine(new File(dir.getName()), f));
			}
		}
		hasFiles |= jarFile(proc, files.getBaseDir(), new File("plugin.xml"));
		return hasFiles;
	}

	private boolean jarFile(RunProcess proc, File dir, File f) {
		if (new File(dir, f.getPath()).isDirectory())
			return false;
		if (blockedByFilters(f))
			return false;
		if (f.getName().startsWith(".git"))
			return false;
		if (proc != null)
		{
			proc.arg("-C");
			proc.arg(dir.getPath());
			proc.arg(f.getPath());
		}
		return true;
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
}
