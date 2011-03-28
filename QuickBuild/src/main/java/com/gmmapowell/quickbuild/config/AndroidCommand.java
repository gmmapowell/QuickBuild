package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.AaptGenBuildCommand;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.build.JavaBuildCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class AndroidCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String projectName;
	private final File projectDir;
	private Project project;
	private AndroidContext acxt;

	@SuppressWarnings("unchecked")
	public AndroidCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public void applyConfig(Config config) {
		project = new Project(projectName, projectDir, config.getOutput());
		acxt = config.getAndroidContext();
	}

	@Override
	public Collection<? extends BuildCommand> buildCommands() {
		List<BuildCommand> ret = new ArrayList<BuildCommand>();
		File manifest = project.getRelative("AndroidManifest.xml");
		File gendir = project.getRelative("gen");
		File resdir = project.getRelative("res");
		AaptGenBuildCommand gen = new AaptGenBuildCommand(acxt, project, manifest, gendir, resdir);
		ret.add(gen);
		JavaBuildCommand genRes = new JavaBuildCommand(project, FileUtils.makeRelativeTo(gendir, project.getBaseDir()).getPath(), "classes");
		ret.add(genRes);
		JavaBuildCommand buildSrc = new JavaBuildCommand(project, "src/main/java", "classes");
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		ret.add(buildSrc);
		return ret;
//		JarBuildCommand jar = new JarBuildCommand(project, project.getName() + ".jar");
//		addJavaBuild(ret, jar, "src/main/java", "classes");
//		JavaBuildCommand junit = addJavaBuild(ret, null, "src/test/java", "test-classes");
//		if (junit != null)
//			junit.addToClasspath(new File(project.getOutputDir(), "classes"));
//		addResources(jar, junit, "src/main/resources");
//		addResources(null, junit, "src/main/resources");
//		addJUnitRun(ret, junit);
//		if (ret.size() == 0)
//			throw new QuickBuildException("None of the required source directories exist");
//		ret.add(jar);
	}

	@Override
	public Project project() {
		return project;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("  android " + projectName + "\n");
		return sb.toString();
	}
}
