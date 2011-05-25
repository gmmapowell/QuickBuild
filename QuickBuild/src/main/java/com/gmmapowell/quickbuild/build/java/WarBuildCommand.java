package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.csharp.XAPResource;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class WarBuildCommand implements Tactic {

	private final WarCommand parent;
	private final File warfile;
	private final StructureHelper files;
	private final List<File> dirsToJar = new ArrayList<File>();
	private final List<PendingResource> warlibs;
	private final List<Pattern> warexcl;
	private final WarResource warResource;

	public WarBuildCommand(WarCommand parent, StructureHelper files, WarResource warResource, String targetName, List<PendingResource> warlibs, List<Pattern> warexcl) {
		this.parent = parent;
		this.files = files;
		this.warResource = warResource;
		this.warlibs = warlibs;
		this.warexcl = warexcl;
		this.warfile = new File(files.getOutputDir(), targetName);
	}

	public void add(File file) {
		dirsToJar.add(file);
	}
	
	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		File tmp = files.getOutput("WebRoot");
		File tmpClasses = files.getOutput("WebRoot/WEB-INF/classes");
		File jarsToDir = files.getOutput("WebRoot/WEB-INF/lib");
		File xapsToDir = files.getOutput("WebRoot");
		FileUtils.cleanDirectory(tmp);
		FileUtils.assertDirectory(tmp);

		// Figure out dependent projects ...
		List<Strategem> str = new ArrayList<Strategem>();
		str.add(parent);
		for (PendingResource r : warlibs)
		{
			copyLib(r, jarsToDir, xapsToDir);
			if (r.getBuiltBy() != null)
				str.add(r.getBuiltBy());
		}
		for (Strategem s : str)
		{
			for (BuildResource r : cxt.getDependencies(s))
			{
				copyLib(r, jarsToDir, xapsToDir);
			}
		}
		
		File root = files.getRelative("WebRoot");
		File classes = files.getRelative("WebRoot/WEB-INF/classes");
		File lib = files.getRelative("WebRoot/WEB-INF/lib");
		boolean worthIt = false;
		for (File f : FileUtils.findFilesMatching(files.getRelative("WebRoot"), "*"))
		{
			if (!f.exists() || f.isDirectory())
				continue;
			if (FileUtils.isUnder(f, classes) || FileUtils.isUnder(f, lib))
				continue;
			if (!FileUtils.isUnder(f, root))
				continue;
			FileUtils.copyAssertingDirs(f, FileUtils.moveRelativeRoot(f, root, tmp));
			worthIt = true;
		}
		
		for (File dir : dirsToJar)
		{
			for (File f : FileUtils.findFilesMatching(dir, "*"))
			{
				if (f.isDirectory())
					continue;

				FileUtils.copyAssertingDirs(f, FileUtils.moveRelativeRoot(f, dir, tmpClasses));
				worthIt = true;
			}
		}


		if (!worthIt)
			return BuildStatus.SKIPPED;
		
		RunProcess proc = new RunProcess("jar");
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.arg("cf");
		proc.arg(warfile.getPath());
		proc.arg("-C");
		proc.arg(tmp.getPath());
		proc.arg(".");

		proc.execute();
		if (proc.getExitCode() == 0)
		{
			cxt.builtResource(warResource);
			return BuildStatus.SUCCESS;
		}
		return BuildStatus.BROKEN;
	}

	private void copyLib(BuildResource r, File jarsToDir, File xapsToDir) {
		if (r instanceof PendingResource)
			r = ((PendingResource)r).physicalResource();
		if (r instanceof JarResource)
		{
			for (Pattern p : warexcl)
				if (p.matcher(r.getPath().getName().toLowerCase()).matches())
					return;
			FileUtils.copyAssertingDirs(r.getPath(), new File(jarsToDir, r.getPath().getName()));
		}
		else if (r instanceof XAPResource)
		{
			for (Pattern p : warexcl)
				if (p.matcher(r.getPath().getName().toLowerCase()).matches())
					return;
			FileUtils.copyAssertingDirs(r.getPath(), new File(xapsToDir, r.getPath().getName()));
		}
		else
			throw new QuickBuildException("Do not know how to include " + r +" of type " + r.getClass() + " inside a WAR");
	}

	@Override
	public String toString() {
		return "Create WAR: " + warResource;
	}
	
	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "war");
	}
}
