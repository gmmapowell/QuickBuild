package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.JarBuildCommand;
import com.gmmapowell.quickbuild.build.java.JavaBuildCommand;
import com.gmmapowell.quickbuild.build.java.JavaSourceDirResource;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;


public class AndroidJarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem {
	private String projectName;
	private final File projectDir;
	private AndroidContext acxt;
	private StructureHelper files;
	private File srcdir;

	@SuppressWarnings("unchecked")
	public AndroidJarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}
	
	@Override
	public AndroidJarCommand applyConfig(Config config) {
		acxt = config.getAndroidContext();
		files = new StructureHelper(projectDir, config.getOutput());
		srcdir = files.getRelative("src/main/java");
		return this;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		// I don't think we need this at the moment
	}

	@Override
	public Collection<? extends Tactic> tactics() {
		List<Tactic> ret = new ArrayList<Tactic>();

		// Hasten, hasten ... cutten and pasten from AndroidCommand
		JavaBuildCommand buildSrc = new JavaBuildCommand(this, files, "src/main/java", "classes");
		buildSrc.dontClean();
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		ret.add(buildSrc);
		
		File resdir = files.getRelative("src/main/resources");
		/* I think this is a bad idea ...
		if (project.getRelative("src/test/java").exists())
		{
			JavaBuildCommand buildTests = new JavaBuildCommand(project, "src/test/java", "test-classes");
			buildTests.addToClasspath(new File(project.getOutputDir(), "classes"));
			buildTests.addToBootClasspath(acxt.getPlatformJar());
			ret.add(buildTests);
			
			if (resdir.exists())
				buildTests.addToClasspath(resdir);
			buildTests.addToClasspath(project.getRelative("src/test/resources"));
			
			JUnitRunCommand junitRun = new JUnitRunCommand(project, buildTests);
			junitRun.addToBootClasspath(acxt.getPlatformJar());
			ret.add(junitRun);
		}
		*/
		JarBuildCommand jar = new JarBuildCommand(this, files, projectName + ".jar");
		jar.add(files.getOutput("classes"));
		if (resdir.exists())
			jar.add(resdir);
		ret.add(jar);

		return ret;
	}

	@Override
	public String toString() {
		return "AndroidJar " + projectName;
	}

	@Override
	public ResourcePacket needsResources() {
		return new ResourcePacket();
	}

	@Override
	public ResourcePacket providesResources() {
		ResourcePacket ret = new ResourcePacket();
		ret.add(new JavaSourceDirResource(this, srcdir));
		return ret;
	}

	@Override
	public ResourcePacket buildsResources() {
		return new ResourcePacket();
	}

	@Override
	public File rootDirectory() {
		return projectDir;
	}

	@Override
	public OrderedFileList sourceFiles() {
		// TODO Auto-generated method stub
		return null;
	}
}
