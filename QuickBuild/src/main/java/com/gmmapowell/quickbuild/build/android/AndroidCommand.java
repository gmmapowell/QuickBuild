package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.build.java.JavaBuildCommand;
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

public class AndroidCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String projectName;
	private final File rootDir;
	private AndroidContext acxt;
	private StructureHelper files;
	private ApkResource apkResource;
	private File apkFile;

	@SuppressWarnings("unchecked")
	public AndroidCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootDir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		files = new StructureHelper(rootDir, config.getOutput());
		acxt = config.getAndroidContext();
		apkFile = files.getOutput(projectName+".apk");
		apkResource = new ApkResource(this, apkFile);
		return this;
	}

	@Override
	public Collection<? extends Tactic> tactics() {
		List<Tactic> ret = new ArrayList<Tactic>();
		File manifest = files.getRelative("src/android/AndroidManifest.xml");
		File gendir = files.getRelative("src/android/gen");
		File resdir = files.getRelative("src/android/res");
		File assetsDir = files.getRelative("src/android/assets");
		File dexFile = files.getOutput("classes.dex");
		File zipfile = files.getOutput(projectName+".ap_");
		
		AaptGenBuildCommand gen = new AaptGenBuildCommand(acxt, manifest, gendir, resdir);
		ret.add(gen);
		JavaBuildCommand genRes = new JavaBuildCommand(this, files, files.makeRelative(gendir).getPath(), "classes", FileUtils.findFilesMatching(gendir, "*.java"));
		genRes.addToBootClasspath(acxt.getPlatformJar());
		ret.add(genRes);
		JavaBuildCommand buildSrc = new JavaBuildCommand(this, files, "src/main/java", "classes", FileUtils.findFilesMatching(files.getRelative("src/main/java"), "*.java"));
		buildSrc.dontClean();
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		ret.add(buildSrc);
		
		// TODO: I feel it should be possible to compile and run unit tests, but what about that bootclasspath?
		if (files.getRelative("src/test/java").exists())
		{
			JavaBuildCommand buildTests = new JavaBuildCommand(this, files, "src/test/java", "test-classes", FileUtils.findFilesMatching(files.getRelative("src/test/java"), "*.java"));
			buildTests.addToClasspath(new File(files.getOutputDir(), "classes"));
			buildTests.addToBootClasspath(acxt.getPlatformJar());
			ret.add(buildTests);
			
			buildTests.addToClasspath(files.getRelative("src/main/resources"));
			buildTests.addToClasspath(files.getRelative("src/test/resources"));
			
			JUnitRunCommand junitRun = new JUnitRunCommand(this, files, buildTests);
			junitRun.addToBootClasspath(acxt.getPlatformJar());
			ret.add(junitRun);
		}
		
		DexBuildCommand dex = new DexBuildCommand(acxt, this, files, files.getOutput("classes"), files.getRelative("src/android/lib"), dexFile);
		for (ConfigApplyCommand cmd : options)
		{
			if (cmd instanceof AndroidUseLibraryCommand)
				((AndroidUseLibraryCommand)cmd).provideTo(dex);
		}
		ret.add(dex);
		AaptPackageBuildCommand pkg = new AaptPackageBuildCommand(acxt, manifest, zipfile, resdir, assetsDir);
		ret.add(pkg);
		ApkBuildCommand apk = new ApkBuildCommand(acxt, zipfile, dexFile, apkFile, apkResource);
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
		return new ResourcePacket();
	}

	@Override
	public ResourcePacket providesResources() {
		return new ResourcePacket();
	}

	@Override
	public ResourcePacket buildsResources() {
		ResourcePacket ret = new ResourcePacket();
		ret.add(apkResource);
		return ret;
	}

	@Override
	public File rootDirectory() {
		return rootDir;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList(new File(rootDir, "src"), "*");
	}

	@Override
	public String identifier() {
		return "BuildApk[" + apkResource.compareAs() +"]";
	}

	@Override
	public boolean onCascade() {
		// TODO Auto-generated method stub
		return false;
	}
}
