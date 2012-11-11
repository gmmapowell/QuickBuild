package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.collections.SetMap;
import com.gmmapowell.exceptions.GPJarException;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildContextAware;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.GPJarEntry;
import com.gmmapowell.utils.GPJarFile;

public class JavaNature implements Nature, BuildContextAware {
	private static class LibDir {
		private final File from;
		private final List<ExcludeCommand> exclusions;

		public LibDir(File from, List<ExcludeCommand> exclusions) {
			this.from = from;
			this.exclusions = exclusions;
		}
	}

	private final List<String> loadedLibs = new ArrayList<String>();
	private final SetMap<String, JarResource> availablePackages = new SetMap<String, JarResource>();
	private final Set<String> duplicates = new HashSet<String>();
	private final ListMap<String, JarResource> projectPackages = new ListMap<String, JarResource>();
	private BuildContext cxt;
	private List<LibDir> libdirs = new ArrayList<LibDir>();
	private final Config conf;
	private final List<String> reportedDuplicates = new ArrayList<String>();

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("boot", BootClassPathCommand.class);
		config.addCommandExtension("exclude", ExcludeCommand.class);
		config.addCommandExtension("include", IncludePackageCommand.class);
		config.addCommandExtension("jar", JarCommand.class);
		config.addCommandExtension("jarjar", JarJarCommand.class);
		config.addCommandExtension("javadoc", JavaDocCommand.class);
		config.addCommandExtension("junitlib", JUnitLibCommand.class);
		config.addCommandExtension("lib", JarLibCommand.class);
		config.addCommandExtension("mainClass", MainClassCommand.class);
		config.addCommandExtension("nojunit", NoJUnitCommand.class);
		config.addCommandExtension("overview", OverviewCommand.class);
		config.addCommandExtension("package", IncludePackageCommand.class);
		config.addCommandExtension("target", SpecifyTargetCommand.class);
		config.addCommandExtension("war", WarCommand.class);
	}

	public JavaNature(Config conf)
	{
		this.conf = conf;
		File libdir = conf.getQuickBuildDir();
		if (libdir == null)
			libdir = FileUtils.getCurrentDir();
		libdirs.add(new LibDir(new File(libdir, "libs"), new ArrayList<ExcludeCommand>()));
	}
	
	@Override
	public void resourceAvailable(BuildResource br, boolean analyze) {
//		if (debug)
//			System.out.println("Available resource: " + br + " analyzing: " + analyze);
		if (!analyze && !br.doAnalysis())
			return;
		
		if (br instanceof JarResource)
			scanJar((JarResource)br);
		else if (br instanceof JavaSourceDirResource)
			rememberSources((JavaSourceDirResource) br);
		else
			throw new UtilException("Can't handle " + br);
	}

	
	private void scanJar(JarResource br) {
		GPJarFile jar;
		try
		{
			jar = new GPJarFile(br.getPath());
		}
		catch (GPJarException ex)
		{
			System.out.println("Could not open jar " + br.getPath());
			ex.printStackTrace();
			return;
		}
//		boolean addedDuplicates = false;
		for (GPJarEntry e : jar)
		{
			if (!e.isClassFile())
				continue;
			String pkg = e.getPackage();
//			if (availablePackages.contains(pkg) && !availablePackages.get(pkg).contains(br))
//			{
//				addedDuplicates = true;
//				duplicates.add(pkg);
//			}
			availablePackages.add(pkg, br);
		}
		jar.close();
//		if (addedDuplicates)
//			showDuplicates();
	}

	private void rememberSources(JavaSourceDirResource br) {
		if (br.getJarResource() == null) {
			return; // useless to us, but surely not an error ...
//			throw new UtilException("Cannot handle JavaSourceDir with no jar");
		}
		List<File> sources = br.getSources();
		HashSet<String> packages = new HashSet<String>();
		for (File f : sources)
		{
			packages.add(FileUtils.convertToDottedName(FileUtils.makeRelativeTo(f.getParentFile(), br.getPath())));
		}
		for (String s : packages)
			projectPackages.add(s, br.getJarResource());
	}

	public void showDuplicates() {
		for (String s : duplicates)
		{
			if (reportedDuplicates.contains(s))
				continue;
			reportedDuplicates.add(s);
			System.out.println("Duplicate/overlapping definitions found for package: " + s);
			for (JarResource f : availablePackages.get(s))
				System.out.println("  " + f);
		}
	}

	public boolean addDependency(Tactic dependent, String needsJavaPackage, String context, boolean debug) {
		// First, try and resolve it with a base jar, or a built jar
		if (availablePackages.contains(needsJavaPackage))
		{
			Set<JarResource> resources = availablePackages.get(needsJavaPackage);
			JarResource haveOne = null;
			for (JarResource jr : resources)
			{
				if (conf.matchesContext(jr, context))
				{
					if (haveOne != null)
						System.out.println("Multiple choices in context " + context + " for package " + needsJavaPackage + ": chose " + haveOne + " and not " + jr);
					else
						haveOne = jr;
				}
			}
			if (haveOne != null)
				return cxt.addDependency(dependent, haveOne, debug);
		}
		
		// OK, try and move the projects around a bit
		if (projectPackages.contains(needsJavaPackage))
		{
			boolean didSomething = false;
			for (JarResource p : projectPackages.get(needsJavaPackage))
			{
				if (p.equals(dependent))
					continue;
				if (p != null && conf.matchesContext(p, context))
					didSomething |= cxt.addDependency(dependent, p, debug);
			}
			return didSomething;
		}

		// It's possible the first reference we come to is a nested class.  Try this hack:
		int idx = needsJavaPackage.lastIndexOf(".");
		if (idx != -1 && Character.isUpperCase(needsJavaPackage.charAt(idx+1)))
			return addDependency(dependent, needsJavaPackage.substring(0,idx), context, debug);

		return false;
//		throw new JavaBuildFailure("cannot find any code that defines package " + needsJavaPackage);
	}

	public boolean isAvailable() {
		return true;
	}

	@Override
	public void done() {
		for (LibDir libdir : libdirs)
			try {
				File dir = libdir.from.getCanonicalFile();
				if (dir.isDirectory())
				{
					searching:
					for (File f : FileUtils.findFilesMatching(dir, "*.jar"))
					{
						for (ExcludeCommand excl : libdir.exclusions)
							if (excl.getPattern().matcher(f.getName()).matches())
							{
								System.out.println("Excluding " + f.getName() + " from " + dir);
								continue searching;
							}
						loadedLibs.add(f.getName());
						conf.resourceAvailable(new JarResource(null, f));
					}
				}
			} catch (IOException e) {
			}
	}

	@Override
	public void info(StringBuilder sb) {
		sb.append("    libdirs: " + libdirs + "\n");
		sb.append("    loaded: " + loadedLibs + "\n");
	}

	@Override
	public void provideBuildContext(BuildContext cxt) {
		this.cxt = cxt;
		cxt.tellMeAbout(this, JarResource.class);
		cxt.tellMeAbout(this, JavaSourceDirResource.class);
	}

	public void addLib(File libsDir, List<ExcludeCommand> exclusions) {
		libdirs.add(new LibDir(libsDir, exclusions));
	}
	
	public void cleanLibDirs()
	{
		libdirs.clear();
	}
}
