package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
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

public class JarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private final ResourcePacket sources = new ResourcePacket();
	protected final ResourcePacket needsResources = new ResourcePacket();
	private String projectName;
	private final File rootdir;
	protected StructureHelper files;
	protected String targetName;
	protected JarResource jarResource;
	protected List<Tactic> tactics;
	private List<File> includePackages;
	private List<File> excludePackages;
	private final List<PendingResource> junitLibs = new ArrayList<PendingResource>();
	private boolean runJunit = true;
	protected ResourcePacket willProvide = new ResourcePacket();

	@SuppressWarnings("unchecked")
	public JarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		files = new StructureHelper(rootdir, config.getOutput());
		
		processOptions();
		jarResource = new JarResource(this, files.getOutput(targetName));

		JarBuildCommand jar = new JarBuildCommand(this, files, FileUtils.ensureExtension(targetName, ".jar"));
		tactics = new ArrayList<Tactic>();
		addJavaBuild(tactics, jar, "src/main/java", "classes");
		JavaBuildCommand junit = addJavaBuild(tactics, null, "src/test/java", "test-classes");
		if (junit != null)
		{
			junit.addToClasspath(new File(files.getOutputDir(), "classes"));
		}
		addResources(jar, junit, "src/main/resources");
		addResources(null, junit, "src/test/resources");
		addJUnitRun(tactics, junit);
		if (tactics.size() == 0)
			throw new QuickBuildException("None of the required source directories exist");
		tactics.add(jar);
		
		additionalCommands(config);

		if (jarResource != null)
			willProvide.add(jarResource);

		return this;
	}

	protected void additionalCommands(Config config) {
		// strategy pattern
	}

	private void processOptions() {
		for (ConfigApplyCommand opt : options)
			if (opt instanceof SpecifyTargetCommand)
			{
				targetName = ((SpecifyTargetCommand) opt).getName();
			}
			else if (opt instanceof IncludePackageCommand)
			{
				includePackage((IncludePackageCommand)opt);
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
		if (targetName == null)
			targetName = projectName + ".jar";
	}

	protected boolean processOption(ConfigApplyCommand opt) {
		return false;
	}

	private void addJUnitLib(JUnitLibCommand opt) {
		junitLibs.add(opt.getResource());
	}

	private void addInvisibleDependency(JUnitLibCommand i) {
		needsResources.add(i.getResource());
	}

	private void includePackage(IncludePackageCommand ipc) {
		if (ipc.exclude)
		{
			if (includePackages != null)
				throw new UtilException("Cannot request both include and exclude packages for " + this);
			if (excludePackages == null)
				excludePackages = new ArrayList<File>();
			excludePackages.add(FileUtils.convertDottedToPath(ipc.pkg));
		}
		else
		{
			if (excludePackages != null)
				throw new UtilException("Cannot request both include and exclude packages for " + this);
			if (includePackages == null)
				includePackages = new ArrayList<File>();
			includePackages.add(FileUtils.convertDottedToPath(ipc.pkg));
		}
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("jar " + projectName);
		return sb.toString();
	}

	@Override
	public Collection<? extends Tactic> tactics() {
		return tactics;
	}

	private JavaBuildCommand addJavaBuild(List<Tactic> accum, JarBuildCommand jar, String src, String bin) {
		File dir = new File(rootdir, src);
		if (dir.isDirectory())
		{
			List<File> sourceFiles;
			if (includePackages != null)
				sourceFiles = FileUtils.findFilesMatchingIncluding(dir, "*.java", includePackages);
			else if (excludePackages != null)
				sourceFiles = FileUtils.findFilesMatchingExcluding(dir, "*.java", excludePackages);
			else
				sourceFiles = FileUtils.findFilesMatching(dir, "*.java");

			JavaBuildCommand ret = new JavaBuildCommand(this, files, src, bin, sourceFiles);
			accum.add(ret);
			
			if (jar != null)
			{
				// Do this for main, but not test ...
				sources.add(new JavaSourceDirResource(this, dir, sourceFiles));
				jar.add(new File(files.getOutputDir(), bin));
			}
			
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

	private void addJUnitRun(List<Tactic> ret, JavaBuildCommand jbc) {
		if (runJunit && jbc != null)
		{
			JUnitRunCommand cmd = new JUnitRunCommand(this, files, jbc);
			cmd.addLibs(junitLibs);
			ret.add(cmd);
		}
	}

	// Certainly the idea is that this is the "static" resouces this guy needs
	// Dynamic resources come in some other way
	@Override
	public ResourcePacket needsResources() {
		return needsResources;
	}

	@Override
	public ResourcePacket providesResources() {
		return sources;
	}

	@Override
	public ResourcePacket buildsResources() {
		return willProvide;
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public OrderedFileList sourceFiles() {
		List<File> files = new ArrayList<File>();
		for (BuildResource br : sources)
		{
			JavaSourceDirResource jsd = (JavaSourceDirResource) br;
			files.addAll(jsd.getSources());
		}
		// TODO: this is a list of JavaSourceDirResource objects.
		// Unpack each of them and then convert that to a list we can use
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

}
