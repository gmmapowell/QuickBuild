package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.zinutils.bytecode.ByteCodeAPI;
import org.zinutils.exceptions.UtilException;

import org.zinutils.parser.LinePatternMatch;
import org.zinutils.parser.LinePatternParser;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.CanBeSkipped;
import com.gmmapowell.quickbuild.build.CareAboutPropagatedDirtyness;
import com.gmmapowell.quickbuild.build.MayPropagateDirtyness;
import com.gmmapowell.quickbuild.build.javascript.JSFileResource;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ProcessResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import org.zinutils.system.RunProcess;
import org.zinutils.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class JavaBuildCommand extends AbstractTactic implements CanBeSkipped, MayPropagateDirtyness, CareAboutPropagatedDirtyness {
	private final File srcdir;
	private final File bindir;
	private final List<PendingResource> resources = new ArrayList<PendingResource>();
	private final BuildClassPath classpath;
	private final BuildClassPath bootclasspath;
	private boolean doClean = true;
	private final List<File> sources;
	private final String label;
	private final String context;
	private final String target;
	private final boolean runAlways;
	private final String idAs;
	private File dumpClasspath;
	private boolean apiChanged = false;

	public JavaBuildCommand(Strategem parent, StructureHelper files, String src, String bin, String label, List<File> sources, String context, String target, boolean runAlways) {
		super(parent);
		this.label = label;
		this.sources = sources;
		this.context = context;
		this.target = target;
		this.runAlways = runAlways;
		this.srcdir = new File(files.getBaseDir(), src);
		this.bindir = new File(files.getOutputDir(), bin);
		if (!bindir.exists())
			if (!bindir.mkdirs())
				throw new QuickBuildException("Cannot build " + srcdir + " because the build directory cannot be created");
		if (bindir.exists() && !bindir.isDirectory())
			throw new QuickBuildException("Cannot build " + srcdir + " because the build directory is not a directory");
		this.classpath = new BuildClassPath();
		this.classpath.add(bindir);
		this.bootclasspath = new BuildClassPath();
		String pn;
		if (parent instanceof JarCommand)
			pn = ((JarCommand)parent).projectName;
		else
			pn = parent.rootDirectory().getName();
		this.idAs = FileUtils.clean(pn);
	}
	
	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList(sources);
	}

	public File getSourceDir() {
		return srcdir;
	}

	public File getOutputDir() {
		return bindir;
	}
	
	public void addResource(PendingResource resource) {
		resources.add(resource);
	}
	
	public void addToClasspath(File file) {
		classpath.add(FileUtils.relativePath(file));
	}

	public void addToBootClasspath(File file) {
		bootclasspath.add(FileUtils.relativePath(file));
	}
	
	public BuildClassPath getClassPath() {
		moveResourcesToClassPath();
		return classpath;
	}

	protected void moveResourcesToClassPath() {
		while (!resources.isEmpty()) {
			PendingResource pr = resources.remove(0);
			classpath.add(pr.getPath());
		}
	}

	public void dontClean()
	{
		doClean = false;
	}

	@Override
	public boolean skipMe(BuildContext cxt) {
		return !runAlways && cxt.doubleQuick;
	}

	public void dumpClasspathTo(File output) {
		dumpClasspath = output;
	}
	
	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		JavaNature nature = cxt.getNature(JavaNature.class);
		if (nature == null || nature == null)
			throw new UtilException("There is no JavaNature installed (huh?)");
		if (!srcdir.isDirectory())
			return BuildStatus.SKIPPED;
		if (!runAlways && cxt.doubleQuick)
			return BuildStatus.SKIPPED;
		if (doClean)
			FileUtils.persistentCleanDirectory(bindir, 10, 300);
		moveResourcesToClassPath();
		classpath.add(bindir);
		for (BuildResource br : cxt.getDependencies(this))
		{
			if (br instanceof JarResource) {
				List<File> fs = ((JarResource)br).getPaths();
				for (File f : fs)
					classpath.add(f);
			} else if (br instanceof DirectoryResource)
				classpath.add(((DirectoryResource)br).getPath());
			else if (br instanceof ProcessResource)
				; // transitive node
			else if (br instanceof JSFileResource)
				; // I don't think we need to "do" anything with this; just use it for dependency analysis
			else
				System.out.println("What do I do with " + br);
		}
		
		if (dumpClasspath != null) {
			try {
				PrintWriter pw = new PrintWriter(dumpClasspath);
				pw.println(classpath.toString());
				pw.close();
			} catch (IOException ex) {
				System.out.println("Could not write classpath to " + dumpClasspath);
			}
		}
		
		RunProcess proc = new RunProcess(System.getenv("JAVA_HOME") + "/bin/javac");
		proc.showArgs(showArgs);
//		proc.showArgs(true);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();
	
		if (!bootclasspath.empty())
		{
			proc.arg("-bootclasspath");
			proc.arg(bootclasspath.toString());
		}
		proc.arg("-sourcepath");
		proc.arg(srcdir.getPath());
		
		// This should really be an option!!!!
		proc.arg("-g");
		proc.arg("-XDignore.symbol.file");
		
		proc.arg("-d");
		proc.arg(bindir.getPath());
		if (!classpath.empty())
		{
			proc.arg("-classpath");
			proc.arg(classpath.toString());
		}
		if (target != null) {
			proc.arg("-source");
			proc.arg(target);
			proc.arg("-target");
			proc.arg(target);
		}
		boolean any = false;
		for (File f : sources)
		{
			proc.arg(f.getPath());
			any = true;
		}
		if (!any)
			return BuildStatus.SKIPPED;
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			// TODO: cxt.addClassDirForProject(project, bindir);
			if ("main".equals(label)) {
				// check whether there were API changes to propagate
				List<String> files = new ArrayList<>();
				for (File f : FileUtils.findFilesMatching(bindir, "*.class")) {
					files.add(f.getPath());
				}
				try {
					File newcf = cxt.getCacheFile("Jar." + idAs + "." + label + ".api.new");
					File oldcf = cxt.getCacheFile("Jar." + idAs + "." + label + ".api");
					PrintWriter pw = new PrintWriter(newcf);
					new ByteCodeAPI().process(pw, files);
					pw.close();
					if (oldcf == null || FileUtils.compare(oldcf, newcf) != 0) {
						FileUtils.copy(newcf, oldcf);
						apiChanged = true;
					} else
						apiChanged = false;
					newcf.delete();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			return BuildStatus.SUCCESS;
		} else if (cxt.grandFallacy) {
			System.out.println("Grand Fallacy mode ... not bothering to fix errors");
			return BuildStatus.BROKEN;
		}
		// compilation errors, usually
		List<String> mypackages = new ArrayList<String>();
		LinePatternParser lpp = new LinePatternParser();
		lpp.match("package ([a-zA-Z0-9_.]*) does not exist", "nopackage", "pkgname");
		lpp.match("cannot access ([a-zA-Z0-9_.]*)\\.[a-zA-Z0-9_]*", "nopackage", "pkgname");
		lpp.match("class file for ([a-zA-Z0-9_.]*)\\.[a-zA-Z0-9_]* not found", "nopackage", "pkgname");
		lpp.match("location: package ([a-zA-Z0-9_.]*)", "nopackage", "pkgname");
		lpp.match("location: class ([a-zA-Z0-9_.]*)\\.[a-zA-Z0-9_]*", "location", "mypackage");
		List<BuildResource> allAdded = new ArrayList<BuildResource>();
		TreeSet<String> missingPackages = new TreeSet<String>();
		for (LinePatternMatch lpm : lpp.applyTo(new StringReader(proc.getStderr())))
		{
			if (lpm.is("nopackage"))
			{
				String pkg = lpm.get("pkgname");
				if (showDebug)
					System.out.println("Looking for " + pkg);
				List<BuildResource> added = nature.addDependency(this, pkg, context, showDebug);
				if (!added.isEmpty())
				{
					if (showDebug)
						System.out.println("  ... added for package " + pkg);
					allAdded.addAll(added);
				}
				else
					missingPackages.add(pkg);
			}
			else if (lpm.is("location"))
				mypackages.add(lpm.get("mypackage"));
			else
				throw new QuickBuildException("Do not know how to handle match " + lpm);
		}
		if (!allAdded.isEmpty())
		{
			System.out.println("       Corrected errors by adding dependencies: " + allAdded);
			return BuildStatus.RETRY;
		} else if (!missingPackages.isEmpty()) {
			System.out.println("       Could not resolve packages: " + missingPackages);
			return BuildStatus.MAYBE_LATER;
		}

		// TODO: this is where I think we want to say "REJECT_TO_BOTTOM_OF_WELL" if there were "nopackage" messages and we didn't add anything

		
		/* This just seems to cause trouble ...
		// There is an element of desperation here, but what can you do?
		// See if we can find other jars that produce the same package as we are currently compiling
		for (String pkg : mypackages)
		{
			if (showDebug)
				System.out.println("Trying to find other implementations of " + pkg);
			if (nature.addDependency(this, pkg, context, showDebug))
			{
				if (showDebug)
					System.out.println("  ... added for package " + pkg);
				cnt++;
			}
		}
		if (cnt > 0)
		{
			System.out.println("       Corrected errors by adding " + cnt + " files with similar packages");
			return BuildStatus.RETRY;
		}
		*/
		cxt.output.buildErrors(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		if (label.equals("main"))
			return "Compiling " + idAs;
		else
			return "Compiling " + label + " classes for " + idAs;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, label);
	}

	@Override
	public boolean dirtynessPropagates() {
		if (!"main".equals(label))
			return false;
		return apiChanged;
	}

	@Override
	public boolean makesDirty(MayPropagateDirtyness dependency) {
		// if it's something I don't understand, play safe ...
		if (!(dependency instanceof JavaBuildCommand) && !(dependency instanceof ArchiveCommand))
			return true;
		
		return dependency.dirtynessPropagates();
	}
}
