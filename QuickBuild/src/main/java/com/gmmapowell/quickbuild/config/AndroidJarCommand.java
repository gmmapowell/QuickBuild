package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.build.JUnitRunCommand;
import com.gmmapowell.quickbuild.build.JarBuildCommand;
import com.gmmapowell.quickbuild.build.JavaBuildCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;


public class AndroidJarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand {
	private String projectName;
	private final File projectDir;
	private Project project;
	private AndroidContext acxt;

	@SuppressWarnings("unchecked")
	public AndroidJarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}
	
	@Override
	public void applyConfig(Config config) {
		project = new Project("android", projectName, projectDir, config.getOutput());
		acxt = config.getAndroidContext();
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		// I don't think we need this at the moment
	}

	@Override
	public Collection<? extends BuildCommand> buildCommands() {
		List<BuildCommand> ret = new ArrayList<BuildCommand>();

		// Hasten, hasten ... cutten and pasten from AndroidCommand
		JavaBuildCommand buildSrc = new JavaBuildCommand(project, "src/main/java", "classes");
		buildSrc.dontClean();
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		ret.add(buildSrc);
		
		File resdir = project.getRelative("src/main/resources");
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

		JarBuildCommand jar = new JarBuildCommand(project, project.getName() + ".jar");
		jar.showArgs(true);
		jar.add(project.getOutput("classes"));
		if (resdir.exists())
			jar.add(resdir);
		ret.add(jar);

		return ret;
	}

	@Override
	public Project project() {
		return project;
	}

}
