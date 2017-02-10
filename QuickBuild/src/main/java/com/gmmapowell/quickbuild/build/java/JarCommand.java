package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.TokenizedLine;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategem;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;

public class JarCommand extends AbstractStrategem {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String projectName;
	private final File rootdir;
	private File bootJar;
	protected StructureHelper files;
	protected String targetName;
	protected List<File> includePackages;
	protected List<File> excludePackages;
	private final List<PendingResource> junitLibs = new ArrayList<PendingResource>();
	private final List<PendingResource> resources = new ArrayList<PendingResource>();
	private final List<String> junitDefines = new ArrayList<String>();
	private final List<String> junitPatterns = new ArrayList<String>();
	private final List<PendingResource> jarLibs = new ArrayList<PendingResource>();
	protected final ResourcePacket<PendingResource> needsResources = new ResourcePacket<PendingResource>();
	private String junitMemory;
	private boolean runJunit = true;
	protected OrderedFileList mainSourceFileList;
	private String javaVersion;
	private final boolean justJunit;
	protected GitIdCommand gitIdCommand;

	public JarCommand(TokenizedLine toks) {
		super(toks, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		justJunit = (toks.cmd().equals("junit"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		config.getNature(JavaNature.class);
		files = new StructureHelper(rootdir, config.getOutput());
		
		javaVersion = config.getVarIfDefined("javaVersion", null);
		processOptions(config);

		ArchiveCommand jar = createAssemblyCommand(figureResourceFiles("src/main/resources", null));
		
		JavaBuildCommand javac;
		if (justJunit)
			javac = null;
		else
			javac = addJavaBuild(tactics, jar, "src/main/java", "classes", "main", true);
		JavaBuildCommand junit = addJavaBuild(tactics, null, "src/test/java", "test-classes", "test", false);
		if (junit != null)
		{
			junit.addToClasspath(new File(files.getOutputDir(), "classes"));
			if (javac != null)
				junit.addProcessDependency(javac);
		}
		addResources(jar, junit, "src/main/resources");
		addResources(null, junit, "src/test/resources");
		JUnitRunCommand jrun = addJUnitRun(tactics, junit);
		if (tactics.size() == 0)
			throw new QuickBuildException("None of the required source directories exist (or have source files) to build " + targetName);
		if (javac != null || jar.alwaysBuild())
			tactics.add(jar);
		if (jrun != null && junit != null)
			jrun.addProcessDependency(junit);
		
		JarResource jarResource = jar.getJarResource();
		if (jarResource != null && javac != null)
			jar.builds(jarResource);

		additionalCommands(config);
		if (javac != null)
			jar.addProcessDependency(javac);
		if (junit != null)
			jar.addProcessDependency(junit);
		return this;
	}

	// strategy pattern
	protected ArchiveCommand createAssemblyCommand(OrderedFileList resourceFiles) {
		return new JarBuildCommand(this, files, targetName, includePackages, excludePackages, resourceFiles, gitIdCommand);
	}

	protected void additionalCommands(Config config) {
		// strategy pattern
	}

	private void processOptions(Config config) {
		for (ConfigApplyCommand opt : options)
		{
			opt.applyTo(config);
			if (opt instanceof SpecifyTargetCommand)
			{
				targetName = ((SpecifyTargetCommand) opt).getName();
			}
			else if (opt instanceof IncludePackageCommand)
			{
				includePackage((IncludePackageCommand)opt);
			}
			else if (opt instanceof JarLibCommand)
			{
				addJarLib((JarLibCommand)opt);
			}
			else if (opt instanceof JUnitLibCommand)
			{
				addJUnitLib((JUnitLibCommand)opt);
			}
			else if (opt instanceof ResourceCommand)
			{
				addResource((ResourceCommand)opt);
			}
			else if (opt instanceof JUnitMemoryCommand)
			{
				setJUnitMemory((JUnitMemoryCommand)opt);
			}
			else if (opt instanceof JUnitDefineCommand)
			{
				addJUnitDefine((JUnitDefineCommand)opt);
			}
			else if (opt instanceof JUnitPatternCommand)
			{
				addJUnitPattern((JUnitPatternCommand)opt);
			}
			else if (opt instanceof NoJUnitCommand)
			{
				runJunit  = false;
			}
			else if (opt instanceof BootClassPathCommand)
			{
				bootJar  = ((BootClassPathCommand)opt).getFile();
			}
			else if (opt instanceof JavaVersionCommand)
			{
				javaVersion = ((JavaVersionCommand)opt).getVersion();
			}
			else if (opt instanceof GitIdCommand)
			{
				if (gitIdCommand != null)
					throw new UtilException("You cannot specify more than one git id variable");
				gitIdCommand = (GitIdCommand) opt;
			}
			else if (processOption(opt))
				;
			else
				throw new UtilException("The option " + opt + " is not valid for JarCommand");
		}
		if (targetName == null)
			targetName = new File(projectName).getName() + ".jar";
	}

	protected boolean processOption(ConfigApplyCommand opt) {
		return false;
	}

	private void addJUnitLib(JUnitLibCommand opt) {
		junitLibs.add(opt.getResource());
	}

	private void addResource(ResourceCommand opt) {
		resources.add(opt.getPendingResource());
		needsResources.add(opt.getPendingResource());
	}

	private void setJUnitMemory(JUnitMemoryCommand opt) {
		junitMemory = opt.getMemory();
	}

	private void addJUnitDefine(JUnitDefineCommand opt) {
		junitDefines.add(opt.getDefine());
	}

	private void addJUnitPattern(JUnitPatternCommand opt) {
		junitPatterns.add(opt.getPattern());
	}

	private void addJarLib(JarLibCommand opt) {
		needsResources.add(opt.getResource());
		jarLibs.add(opt.getResource());
	}

	private void includePackage(IncludePackageCommand ipc) {
		if (ipc.isExclude())
		{
			if (includePackages != null)
				throw new UtilException("Cannot request both include and exclude packages for " + this);
			if (excludePackages == null)
				excludePackages = new ArrayList<File>();
			excludePackages.add(FileUtils.convertDottedToPath(ipc.getPackage()));
		}
		else
		{
			if (excludePackages != null)
				throw new UtilException("Cannot request both include and exclude packages for " + this);
			if (includePackages == null)
				includePackages = new ArrayList<File>();
			includePackages.add(FileUtils.convertDottedToPath(ipc.getPackage()));
		}
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Jar " + targetName);
		return sb.toString();
	}

	private OrderedFileList figureResourceFiles(String main, String test) {
		OrderedFileList ret = new OrderedFileList();
		addFiles(ret, main);
		addFiles(ret, test);
		if (ret.isEmpty())
			return null;
		return ret;
	}

	protected void addFiles(OrderedFileList ret, String resdir) {
		if (resdir == null)
			return;
		File dir = new File(rootdir, resdir);
		if (dir.isDirectory())
		{
			List<File> allFiles = FileUtils.findFilesMatching(dir, "*");
			List<File> sourceFiles;
			if (includePackages != null)
				sourceFiles = FileUtils.findFilesMatchingIncluding(dir, "*", includePackages);
			else if (excludePackages != null)
				sourceFiles = FileUtils.findFilesMatchingExcluding(dir, "*", excludePackages);
			else
				sourceFiles = allFiles;

			ret.add(sourceFiles);
		}
	}

	private JavaBuildCommand addJavaBuild(List<? super Tactic> accum, ArchiveCommand jar, String src, String bin, String label, boolean runAlways) {
		File dir = new File(rootdir, src);
		if (dir.isDirectory())
		{
			List<File> allFiles = FileUtils.findFilesMatching(dir, "*.java");
			List<File> sourceFiles;
			if (includePackages != null)
				sourceFiles = FileUtils.findFilesMatchingIncluding(dir, "*.java", includePackages);
			else if (excludePackages != null)
				sourceFiles = FileUtils.findFilesMatchingExcluding(dir, "*.java", excludePackages);
			else
				sourceFiles = allFiles;

			if (sourceFiles.size() == 0)
				return null;
			
			JavaBuildCommand ret = new JavaBuildCommand(this, files, src, bin, label, sourceFiles, "jdk", javaVersion, runAlways);
			if (bootJar != null)
				ret.addToBootClasspath(bootJar);
			accum.add(ret);
			for (PendingResource br : needsResources)
				ret.needs(br);
			
			JavaSourceDirResource sourcesResource = new JavaSourceDirResource(dir, sourceFiles);
			ret.provides(sourcesResource);

			if (jar != null)
			{
				sourcesResource.buildsInto(jar.getJarResource());

				// Do this for main, but not test ...
				OrderedFileList tmp = ret.sourceFiles();
				if (mainSourceFileList == null)
					mainSourceFileList = tmp;
				else
					mainSourceFileList.add(tmp);
				File resdir = new File(dir.getParentFile(), "resources");
				if (resdir.isDirectory())
					mainSourceFileList.add(resdir, "*");
				
				jar.add(new File(files.getOutputDir(), bin));
			}
			
			return ret;
		}
		return null;
	}
	
	private void addResources(ArchiveCommand jar, JavaBuildCommand junit, String src) {
		File dir = new File(rootdir, src);
		if (dir.isDirectory())
		{
			if (jar != null)
				jar.add(dir);
			if (junit != null)
				junit.addToClasspath(dir);
		}
	}

	private JUnitRunCommand addJUnitRun(List<? super Tactic> ret, JavaBuildCommand jbc) {
		if (runJunit && jbc != null)
		{
			JUnitRunCommand cmd = new JUnitRunCommand(this, files, jbc, figureResourceFiles("src/main/resources", "src/test/resources"));
			cmd.addLibs(junitLibs);
			if (junitMemory != null)
				cmd.setJUnitMemory(junitMemory);
			for (String d : junitDefines)
				cmd.define(d);
			for (String d : junitPatterns)
				cmd.pattern(d);
			ret.add(cmd);
			return cmd;
		}
		return null;
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public String identifier() {
		return "Jar[" + targetName + "]";
	}

	@Override
	public boolean onCascade() {
		return false;
	}
}
