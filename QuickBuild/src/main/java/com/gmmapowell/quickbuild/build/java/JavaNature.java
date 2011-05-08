package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.collections.SetMap;
import com.gmmapowell.exceptions.GPJarException;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildContextAware;
import com.gmmapowell.quickbuild.build.StrategemResource;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigFactory;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Nature;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.GPJarEntry;
import com.gmmapowell.utils.GPJarFile;

public class JavaNature implements Nature, BuildContextAware {
	private final List<String> loadedLibs = new ArrayList<String>();
	private final Map<String, JarResource> availablePackages = new HashMap<String, JarResource>();
	private final SetMap<String, BuildResource> duplicates = new SetMap<String, BuildResource>();
	private final ListMap<String, Strategem> projectPackages = new ListMap<String, Strategem>();
	private BuildContext cxt;
	private List<File> libdirs = new ArrayList<File>();
	private final Config conf;

	public static void init(ConfigFactory config)
	{
		config.addCommandExtension("exclude", ExcludeCommand.class);
		config.addCommandExtension("file", WarRandomFileCommand.class);
		config.addCommandExtension("jar", JarCommand.class);
		config.addCommandExtension("javadoc", JavaDocCommand.class);
		config.addCommandExtension("junitlib", JUnitLibCommand.class);
		config.addCommandExtension("lib", WarLibCommand.class);
		config.addCommandExtension("nojunit", NoJUnitCommand.class);
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
		libdirs.add(new File(libdir, "libs"));
	}
	
	@Override
	public void resourceAvailable(BuildResource br) {
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
			return;
		}
		boolean addedDuplicates = false;
		for (GPJarEntry e : jar)
		{
			if (!e.isClassFile())
				continue;
			String pkg = e.getPackage();
			if (!availablePackages.containsKey(pkg))
			{
				availablePackages.put(pkg, br);
			}
			else if (availablePackages.get(pkg).equals(br))
				continue;
			else
			{
				if (!duplicates.contains(pkg))
					duplicates.add(pkg, availablePackages.get(pkg));
				duplicates.add(pkg, br);
				addedDuplicates = true;
			}
		}
		if (addedDuplicates)
			showDuplicates();
	}

	private void rememberSources(JavaSourceDirResource br) {
		if (br.getBuiltBy() == null)
			throw new UtilException("Cannot handle JavaSourceDir with no builder");
		List<File> sources = br.getSources();
		HashSet<String> packages = new HashSet<String>();
		for (File f : sources)
		{
			packages.add(FileUtils.convertToDottedName(FileUtils.makeRelativeTo(f.getParentFile(), br.getPath())));
		}
		for (String s : packages)
			projectPackages.add(s, br.getBuiltBy());
	}

	public void showDuplicates() {
		for (String s : duplicates)
		{
			System.out.println("Duplicate/overlapping definitions found for package: " + s);
			for (BuildResource f : duplicates.get(s))
				System.out.println("  " + f);
		}
	}

	public boolean addDependency(Strategem dependent, String needsJavaPackage) {
		// First, try and resolve it with a base jar, or a built jar
		if (availablePackages.containsKey(needsJavaPackage))
		{
			JarResource resource = availablePackages.get(needsJavaPackage);
			return cxt.addDependency(dependent, resource);
		}
		
		// OK, try and move the projects around a bit
		if (projectPackages.contains(needsJavaPackage))
		{
			boolean didSomething = false;
			for (Strategem p : projectPackages.get(needsJavaPackage))
			{
				didSomething |= cxt.addDependency(dependent, new StrategemResource(p));
			}
			return didSomething;
		}

		// It's possible the first reference we come to is a nested class.  Try this hack:
		int idx = needsJavaPackage.lastIndexOf(".");
		if (idx != -1 && Character.isUpperCase(needsJavaPackage.charAt(idx+1)))
			return addDependency(dependent, needsJavaPackage.substring(0,idx));

		throw new JavaBuildFailure("cannot find any code that defines package " + needsJavaPackage);
	}

	public boolean isAvailable() {
		return true;
	}

	@Override
	public void done() {
		for (File libdir : libdirs)
			try {
				libdir = libdir.getCanonicalFile();
				if (libdir.isDirectory())
				{
					for (File f : FileUtils.findFilesMatching(libdir, "*.jar"))
					{
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

	public void addLib(File libsDir) {
		libdirs.add(libsDir);
	}
	
	public void cleanLibDirs()
	{
		libdirs.clear();
	}
}
