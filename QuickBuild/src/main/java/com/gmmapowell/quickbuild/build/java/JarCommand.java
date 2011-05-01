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
import com.gmmapowell.quickbuild.config.SpecifyTargetCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
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
	private String projectName;
	private final File rootdir;
	private StructureHelper files;
	private String targetName;
	private JarResource jarResource;
	private List<Tactic> tactics;
	private List<File> includePackages;
	private List<File> excludePackages;

	@SuppressWarnings("unchecked")
	public JarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		System.out.println("Applying config to " + this);
		files = new StructureHelper(rootdir, config.getOutput());
		
		processOptions();
		jarResource = new JarResource(this, files.getOutput(targetName));

		JarBuildCommand jar = new JarBuildCommand(this, files, targetName);
		tactics = new ArrayList<Tactic>();
		addJavaBuild(tactics, jar, "src/main/java", "classes");
		JavaBuildCommand junit = addJavaBuild(tactics, null, "src/test/java", "test-classes");
		if (junit != null)
			junit.addToClasspath(new File(files.getOutputDir(), "classes"));
		addResources(jar, junit, "src/main/resources");
		addResources(null, junit, "src/test/resources");
		addJUnitRun(tactics, junit);
		if (tactics.size() == 0)
			throw new QuickBuildException("None of the required source directories exist");
		tactics.add(jar);

		return this;
	}

	private void processOptions() {
		for (ConfigApplyCommand i : options)
			if (i instanceof SpecifyTargetCommand)
			{
				targetName = ((SpecifyTargetCommand) i).getName();
				return;
			}
			else if (i instanceof IncludePackageCommand)
			{
				includePackage((IncludePackageCommand)i);
			}
			else
				throw new UtilException("The option " + i + " is not valid for JarCommand");
		targetName = projectName + ".jar";
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
		System.out.println("Adding child " + obj);
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
				// This is the case for main, but not test ...
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
		if (jbc != null)
		{
			ret.add(new JUnitRunCommand(this, files, jbc));
		}
	}

	// Certainly the idea is that this is the "static" resouces this guy needs
	// Dynamic resources come in some other way
	@Override
	public ResourcePacket needsResources() {
		return new ResourcePacket();
	}

	@Override
	public ResourcePacket providesResources() {
		return sources;
	}

	@Override
	public ResourcePacket buildsResources() {
		ResourcePacket ret = new ResourcePacket();
		ret.add(jarResource);
		return ret;
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
