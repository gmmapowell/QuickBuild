package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.AaptGenBuildCommand;
import com.gmmapowell.quickbuild.build.AaptPackageBuildCommand;
import com.gmmapowell.quickbuild.build.ApkBuildCommand;
import com.gmmapowell.quickbuild.build.DexBuildCommand;
import com.gmmapowell.quickbuild.build.JUnitRunCommand;
import com.gmmapowell.quickbuild.build.JavaBuildCommand;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class AndroidCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String projectName;
	private final File projectDir;
	private AndroidContext acxt;
	private StructureHelper files;

	@SuppressWarnings("unchecked")
	public AndroidCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		files = new StructureHelper(projectDir, config.getOutput());
		acxt = config.getAndroidContext();
		return this;
	}

	@Override
	public Collection<? extends Tactic> tactics() {
		List<Tactic> ret = new ArrayList<Tactic>();
		File manifest = files.getRelative("AndroidManifest.xml");
		File gendir = files.getRelative("gen");
		File resdir = files.getRelative("res");
		File assetsDir = files.getRelative("assets");
		File dexFile = files.getOutput("classes.dex");
		File zipfile = files.getOutput(projectName+".ap_");
		File apkFile = files.getOutput(projectName+".apk");
		
		AaptGenBuildCommand gen = new AaptGenBuildCommand(acxt, manifest, gendir, resdir);
		ret.add(gen);
		JavaBuildCommand genRes = new JavaBuildCommand(this, files, files.makeRelative(gendir).getPath(), "classes");
		genRes.addToBootClasspath(acxt.getPlatformJar());
		ret.add(genRes);
		JavaBuildCommand buildSrc = new JavaBuildCommand(this, files, "src/main/java", "classes");
		buildSrc.dontClean();
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		ret.add(buildSrc);
		
		// TODO: I feel it should be possible to compile and run unit tests, but what about that bootclasspath?
		if (files.getRelative("src/test/java").exists())
		{
			JavaBuildCommand buildTests = new JavaBuildCommand(this, files, "src/test/java", "test-classes");
			buildTests.addToClasspath(new File(files.getOutputDir(), "classes"));
			buildTests.addToBootClasspath(acxt.getPlatformJar());
			ret.add(buildTests);
			
			buildTests.addToClasspath(files.getRelative("src/main/resources"));
			buildTests.addToClasspath(files.getRelative("src/test/resources"));
			
			JUnitRunCommand junitRun = new JUnitRunCommand(this, files, buildTests);
			junitRun.addToBootClasspath(acxt.getPlatformJar());
			ret.add(junitRun);
		}
		
		DexBuildCommand dex = new DexBuildCommand(acxt, this, files, files.getOutput("classes"), dexFile);
		for (ConfigApplyCommand cmd : options)
		{
			if (cmd instanceof AndroidUseLibraryCommand)
				((AndroidUseLibraryCommand)cmd).provideTo(dex);
		}
		ret.add(dex);
		AaptPackageBuildCommand pkg = new AaptPackageBuildCommand(acxt, manifest, zipfile, resdir, assetsDir);
		ret.add(pkg);
		ApkBuildCommand apk = new ApkBuildCommand(acxt, zipfile, dexFile, apkFile); // TODO: pass down apk as a resource
		ret.add(apk);
		return ret;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("android " + projectName);
		return sb.toString();
	}

	@Override
	public ResourcePacket needsResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourcePacket providesResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourcePacket buildsResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File rootDirectory() {
		// TODO Auto-generated method stub
		return null;
	}
}
