package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.FileUtils;

public class JarBuildCommand extends ArchiveCommand {
	private final GitIdCommand gitIdCommand;
	private final String idAs;

	public JarBuildCommand(Strategem parent, StructureHelper files, String targetName, List<File> includePackages, List<File> excludePackages, GitIdCommand gitIdCommand) {
		super(parent, includePackages, excludePackages);
		this.gitIdCommand = gitIdCommand;
		this.jarResource = new JarResource(this, files.getOutput(FileUtils.ensureExtension(targetName, ".jar")));
		this.jarfile = this.jarResource.getFile();
		this.idAs = parent.rootDirectory().getName();
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		try {
			if (jarfile.exists() && !jarfile.delete())
				throw new QuickBuildException("Could not delete " + jarfile);
			if (showDebug)
				System.out.println("Writing JAR file to " + jarfile.getPath());
			JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarfile.getPath()));
			if (gitIdCommand != null)
				gitIdCommand.writeTrackerFile(jos, "META-INF");
			boolean hasFiles = writeFilesToJar(jos, showDebug);
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
			jos.close();
			cxt.builtResource(jarResource);
			return BuildStatus.SUCCESS;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	boolean writeFilesToJar(JarOutputStream jos, boolean showDebug) throws IOException {
		boolean hasFiles = false;
		for (File dir : dirsToJar)
		{
			for (File f : FileUtils.findFilesMatching(dir, "*"))
			{
				if (!f.exists() || f.isDirectory())
					continue;
				if (blockedByFilters(f))
					continue;
				if (f.getName().startsWith(".git"))
					continue;
				if (showDebug)
					System.out.println("Adding " + f + " to jar");
				writeToJar(jos, f, FileUtils.makeRelativeTo(f, dir));
				hasFiles = true;
			}
		}
		return hasFiles;
	}

	private void writeToJar(JarOutputStream jos, File f, File relative) throws IOException {
		JarEntry je = new JarEntry(relative.getPath().replaceAll("\\\\", "/"));
		jos.putNextEntry(je);
		FileUtils.copyFileToStream(f, jos);
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
		return "Creating jar " + idAs;
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public boolean analyzeExports() {
		return true;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "jar");
	}
}
