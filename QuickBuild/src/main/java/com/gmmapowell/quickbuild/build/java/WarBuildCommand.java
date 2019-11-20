package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.csharp.XAPResource;
import com.gmmapowell.quickbuild.config.DirectoryResourceCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

import org.zinutils.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class WarBuildCommand extends ArchiveCommand {
	private final File warfile;
	private final StructureHelper files;
	private final List<PendingResource> warlibs;
	private final List<DirectoryResourceCommand> wardirs;
	private final List<Pattern> warexcl;
	private final WarResource warResource;
	private GitIdCommand gitIdCommand;
	private final File webRoot;

	public WarBuildCommand(WarCommand parent, StructureHelper files, String targetName, List<PendingResource> warlibs, List<DirectoryResourceCommand> wardirs, List<Pattern> warexcl, OrderedFileList ofl, GitIdCommand gitIdCommand) {
		super(parent, null, null, ofl, null, null);
		this.files = files;
		this.warlibs = warlibs;
		this.wardirs = wardirs;
		this.warexcl = warexcl;
		this.gitIdCommand = gitIdCommand;
		this.warfile = new File(files.getOutputDir(), targetName);
		this.warResource = new WarResource(this, files.getOutput(targetName));
		webRoot = files.getRelative("WebRoot");
		if (webRoot != null && webRoot.isDirectory())
			for (File f : FileUtils.findFilesUnderMatching(webRoot, "*"))
				if (f.isFile())
					resourceFiles.add(f);
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	public WarResource getResource() {
		return warResource;
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		try
		{
			if (showDebug)
				System.out.println("WAR to file: " + warfile);
			JarOutputStream jos = new JarOutputStream(new FileOutputStream(warfile.getPath()));
			if (gitIdCommand != null)
				gitIdCommand.writeTrackerFile(cxt, jos, "WEB-INF/classes", identifier());
	
			// Copy the local items - WebRoot, classes and resources
			boolean worthIt = addOurFiles(jos, webRoot, "", false, showDebug);
			worthIt |= addOurFiles(jos, files.getRelative("src/main/resources"), "WEB-INF/", worthIt, showDebug);
			worthIt |= addOurFiles(jos, files.getRelative("qbout/classes"), "WEB-INF/classes/", worthIt, showDebug);
			
			// Now find all the dependencies
			List<Tactic> str = new ArrayList<Tactic>();
			str.add(this); // add this project
			
			// Now look at all the dependent resources
			TreeSet<LibEntry> libs = new TreeSet<LibEntry>();
			for (PendingResource r : warlibs)
			{
				addLibs(libs, r, showDebug);
				if (r.getBuiltBy() != null)
					str.add(r.getBuiltBy());
			}
			for (DirectoryResourceCommand r : wardirs)
			{
				addLibs(libs, r.getResource(), showDebug);
//				if (r.getBuiltBy() != null)
//					str.add(r.getBuiltBy());
			}
			for (Tactic t : str)
			{
				for (BuildResource r : cxt.getDependencies(t))
				{
					addLibs(libs, r, showDebug);
				}
			}
	
			for (LibEntry le : libs)
			{
				if (showDebug)
					System.out.println("  adding LibEntry " + le.from);
				le.writeTo(jos);
			}
			
			if (!worthIt)
			{
				// this can throw an error if nothing was written - so catch it
				try { jos.close(); } catch (Exception ex) { }
				return BuildStatus.SKIPPED;
			}
			
			jos.close();
			cxt.builtResource(warResource);
			return BuildStatus.SUCCESS;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return BuildStatus.BROKEN;
		}
	}

	private void addLibs(TreeSet<LibEntry> libs, BuildResource r, boolean showDebug) {
		if (r instanceof PendingResource)
			r = ((PendingResource)r).physicalResource();
		if (r instanceof JarResource)
		{
			for (Pattern p : warexcl)
				if (p.matcher(r.getPath().getName().toLowerCase()).matches())
					return;
			if (showDebug)
				System.out.println("  adding lib " + r.getPath());
			libs.add(new LibEntry("WEB-INF/lib/" + r.getPath().getName(), r.getPath()));
		}
		else if (r instanceof DirectoryResource) {
			libs.add(new LibEntry("WEB-INF/classes/", r.getPath()));
		}
		else if (r instanceof XAPResource)
		{
			for (Pattern p : warexcl)
				if (p.matcher(r.getPath().getName().toLowerCase()).matches())
					return;
			if (showDebug)
				System.out.println("   adding lib " + r.getPath());
			libs.add(new LibEntry(r.getPath().getName(), r.getPath()));
		}
		else if (r instanceof ProcessResource) {
//			System.out.println("Ignoring " + r);
		}
		else
			throw new QuickBuildException("Do not know how to include " + r +" of type " + r.getClass() + " inside a WAR");
		
	}

	private boolean addOurFiles(JarOutputStream jos, File root, String prefix, boolean worthIt, boolean showDebug) throws IOException {
		if (root.isDirectory()) {
			nextFile:
			for (File f : FileUtils.findFilesMatching(root, "*"))
			{
				if (!f.exists() || f.isDirectory())
					continue;
				if (!FileUtils.isUnder(f, root))
					continue;
				if (f.getName().startsWith("."))
					continue;
				for (Pattern p : warexcl) {
					if (p.matcher(f.getName().toLowerCase()).matches())
						continue nextFile;
				}
				if (showDebug)
					System.out.println("  adding " + f);
				writeToJar(jos, f, prefix, FileUtils.makeRelativeTo(f, root));
				worthIt = true;
			}
		}
		return worthIt;
	}

	private void writeToJar(JarOutputStream jos, File f, String prefix, File relative) throws IOException {
		JarEntry je = new JarEntry(prefix + relative.getPath().replaceAll("\\\\", "/"));
		jos.putNextEntry(je);
		FileUtils.copyFileToStream(f, jos);
	}

	@Override
	public String toString() {
		return "Create WAR: " + warResource;
	}
	
	@Override
	public boolean alwaysBuild() {
		return true;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "war");
	}

	class LibEntry implements Comparable<LibEntry> {
		private File from;
		private final File relative;

		public LibEntry(String entry, File path) {
			this.relative = new File(entry);
			from = path;
		}

		public void writeTo(JarOutputStream jos) throws IOException {
			if (from.isDirectory()) {
				for (File q : FileUtils.findFilesUnderMatching(from, "*")) {
					File fq = new File(from, q.getPath());
					if (fq.isDirectory())
						continue;
					writeToJar(jos, fq, "", FileUtils.combine(relative, q));
				}
			} else
				writeToJar(jos, from, "", relative);
		}

		@Override
		public int compareTo(LibEntry o) {
			return from.getPath().compareTo(o.from.getPath());
		}
		
		@Override
		public String toString() {
			return "LibEntry[" + from.getPath() + "]";
		}
	}
}

