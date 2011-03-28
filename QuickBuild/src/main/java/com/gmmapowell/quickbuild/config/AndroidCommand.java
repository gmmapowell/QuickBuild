package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.AaptGenBuildCommand;
import com.gmmapowell.quickbuild.build.AaptPackageBuildCommand;
import com.gmmapowell.quickbuild.build.ApkBuildCommand;
import com.gmmapowell.quickbuild.build.BuildCommand;
import com.gmmapowell.quickbuild.build.DexBuildCommand;
import com.gmmapowell.quickbuild.build.JUnitRunCommand;
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
		project = new Project("android", projectName, projectDir, config.getOutput());
		acxt = config.getAndroidContext();
	}

	@Override
	public Collection<? extends BuildCommand> buildCommands() {
		List<BuildCommand> ret = new ArrayList<BuildCommand>();
		File manifest = project.getRelative("AndroidManifest.xml");
		File gendir = project.getRelative("gen");
		File resdir = project.getRelative("res");
		File dexFile = project.getOutput("classes.dex");
		File zipfile = project.getOutput(project.getName()+".ap_");
		File apkFile = project.getOutput(project.getName()+".apk");
		
		AaptGenBuildCommand gen = new AaptGenBuildCommand(acxt, project, manifest, gendir, resdir);
		ret.add(gen);
		JavaBuildCommand genRes = new JavaBuildCommand(project, project.makeRelative(gendir).getPath(), "classes");
		genRes.addToBootClasspath(acxt.getPlatformJar());
		genRes.showArgs(true);
		ret.add(genRes);
		JavaBuildCommand buildSrc = new JavaBuildCommand(project, "src/main/java", "classes");
		buildSrc.dontClean();
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		ret.add(buildSrc);
		
		// TODO: I feel it should be possible to compile and run unit tests, but what about that bootclasspath?
		if (project.getRelative("src/test/java").exists())
		{
			JavaBuildCommand buildTests = new JavaBuildCommand(project, "src/test/java", "test-classes");
			buildTests.addToClasspath(new File(project.getOutputDir(), "classes"));
			buildTests.addToBootClasspath(acxt.getPlatformJar());
			ret.add(buildTests);
			
			buildTests.addToClasspath(project.getRelative("src/main/resources"));
			buildTests.addToClasspath(project.getRelative("src/test/resources"));
			
			JUnitRunCommand junitRun = new JUnitRunCommand(project, buildTests);
			junitRun.addToBootClasspath(acxt.getPlatformJar());
			ret.add(junitRun);
		}
		
		DexBuildCommand dex = new DexBuildCommand(acxt, project, project.getOutput("classes"), dexFile);
		ret.add(dex);
		AaptPackageBuildCommand pkg = new AaptPackageBuildCommand(acxt, project, manifest, zipfile, resdir);
		ret.add(pkg);
		ApkBuildCommand apk = new ApkBuildCommand(acxt, project, zipfile, dexFile, apkFile);
		ret.add(apk);
		return ret;
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
