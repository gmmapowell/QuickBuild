package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.HasAncillaryFiles;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class JarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, HasAncillaryFiles {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private final ResourcePacket<BuildResource> sources = new ResourcePacket<BuildResource>();
	protected final ResourcePacket<PendingResource> needsResources = new ResourcePacket<PendingResource>();
	private String projectName;
	private final File rootdir;
	protected StructureHelper files;
	protected String targetName;
	protected JarResource jarResource;
	protected List<Tactic> tactics;
	private List<File> includePackages;
	private List<File> excludePackages;
	private final List<PendingResource> junitLibs = new ArrayList<PendingResource>();
	private final List<PendingResource> jarLibs = new ArrayList<PendingResource>();
	private boolean runJunit = true;
	protected ResourcePacket<BuildResource> willProvide = new ResourcePacket<BuildResource>();
	private JavaSourceDirResource mainSources;
	private JavaSourceDirResource testSources;
	private OrderedFileList mainSourceFileList;

	@SuppressWarnings("unchecked")
	public JarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		config.getNature(JavaNature.class);
		files = new StructureHelper(rootdir, config.getOutput());
		
		processOptions(config);

		JarBuildCommand jar = new JarBuildCommand(this, files, targetName, includePackages, excludePackages);
		jarResource = jar.getJarResource();
		
		tactics = new ArrayList<Tactic>();
		JavaBuildCommand javac = addJavaBuild(tactics, jar, "src/main/java", "classes", "main");
		JavaBuildCommand junit = addJavaBuild(tactics, null, "src/test/java", "test-classes", "test");
		if (junit != null)
		{
			junit.addToClasspath(new File(files.getOutputDir(), "classes"));
			junit.addProcessDependency(javac);
		}
		addResources(jar, junit, "src/main/resources");
		addResources(null, junit, "src/test/resources");
		JUnitRunCommand jrun = addJUnitRun(tactics, junit);
		if (tactics.size() == 0)
			throw new QuickBuildException("None of the required source directories exist (or have source files) to build " + targetName);
		tactics.add(jar);
		if (jrun != null)
			jrun.addProcessDependency(junit);
		
		additionalCommands(config);

		if (jarResource != null)
			willProvide.add(jarResource);
		else if (junit != null)
		{
			JUnitResource jur = new JUnitResource(junit, files.getOutput(FileUtils.ensureExtension(targetName, ".junr")));
			willProvide.add(jur);
			jrun.writeTo(jur);
		}

		jar.addProcessDependency(javac);
		jar.addProcessDependency(jrun);
		return this;
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
			else if (opt instanceof NoJUnitCommand)
			{
				runJunit  = false;
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

	@Override
	public List<? extends Tactic> tactics() {
		return tactics;
	}

	private JavaBuildCommand addJavaBuild(List<Tactic> accum, JarBuildCommand jar, String src, String bin, String label) {
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
			
			JavaBuildCommand ret = new JavaBuildCommand(this, files, src, bin, label, sourceFiles, "jdk");
			accum.add(ret);
			
			JavaSourceDirResource sourcesResource = new JavaSourceDirResource(dir, sourceFiles);
			sources.add(sourcesResource);
			
			if (jar != null)
			{
				sourcesResource.buildsInto(jar.getJarResource());

				// Do this for main, but not test ...
				mainSources = sourcesResource;
				mainSourceFileList = mapOFL(mainSources);
				File resdir = new File(dir.getParentFile(), "resources");
				if (resdir.isDirectory())
					mainSourceFileList.add(resdir, "*");
				
				jar.add(new File(files.getOutputDir(), bin));
			}
			else
				testSources = sourcesResource;
			
			return ret;
		}
		return null;
	}
	
	private void addResources(JarBuildCommand jar, JavaBuildCommand junit, String src) {
		File dir = new File(rootdir, src);
		if (dir.isDirectory())
		{
			if (jar != null)
				jar.add(dir);
			if (junit != null)
				junit.addToClasspath(dir);
		}
	}

	private JUnitRunCommand addJUnitRun(List<Tactic> ret, JavaBuildCommand jbc) {
		if (runJunit && jbc != null)
		{
			JUnitRunCommand cmd = new JUnitRunCommand(this, files, jbc);
			cmd.addLibs(junitLibs);
			ret.add(cmd);
			return cmd;
		}
		return null;
	}

	// Certainly the idea is that this is the "static" resouces this guy needs
	// Dynamic resources come in some other way
	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needsResources;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return sources;
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return willProvide;
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return mainSourceFileList;
	}
	
	@Override
	public OrderedFileList getAncillaryFiles() {
		return mapOFL(testSources);
	}
	
	public OrderedFileList mapOFL(JavaSourceDirResource jsd)
	{
		List<File> files = new ArrayList<File>();
		if (jsd != null)
			files.addAll(jsd.getSources());
		return new OrderedFileList(files);
	}

	@Override
	public String identifier() {
		return "Jar[" + targetName + "]";
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public boolean analyzeExports() {
		return true;
	}

}
