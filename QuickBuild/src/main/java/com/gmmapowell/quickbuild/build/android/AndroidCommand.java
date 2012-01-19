package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gmmapowell.bytecode.JavaRuntimeReplica;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.DeferredFileList;
import com.gmmapowell.quickbuild.build.java.ExcludeCommand;
import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.build.java.JavaBuildCommand;
import com.gmmapowell.quickbuild.build.java.JavaNature;
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
	private ResourcePacket<PendingResource> uselibs = new ResourcePacket<PendingResource>();
	private ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private Set<Pattern> exclusions = new HashSet<Pattern>();
	final JavaRuntimeReplica jrr;
	private ArrayList<Tactic> tactics;
	private boolean jrrConfigured = false;
	private File bindir;

	@SuppressWarnings("unchecked")
	public AndroidCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootDir = FileUtils.findDirectoryNamed(projectName);
		this.jrr = new JavaRuntimeReplica();
	}

	@Override
	public Strategem applyConfig(Config config) {
		config.getNature(JavaNature.class);
		config.getNature(AndroidNature.class);
		files = new StructureHelper(rootDir, config.getOutput());
		acxt = config.getAndroidContext();
		apkFile = files.getOutput(projectName+".apk");
		apkResource = new ApkResource(this, apkFile);
		
		for (ConfigApplyCommand cmd : options)
		{
			cmd.applyTo(config);
			if (cmd instanceof AndroidUseLibraryCommand)
			{
				PendingResource pr = ((AndroidUseLibraryCommand)cmd).getResource();
				uselibs.add(pr);
				needs.add(pr);
			}
			else if (cmd instanceof ExcludeCommand)
			{
				exclusions.add(((ExcludeCommand) cmd).getPattern());
			}
			else
				throw new UtilException("Cannot handle " + cmd);
		}

		return this;
	}

	@Override
	public List<? extends Tactic> tactics() {
		if (tactics != null)
			return tactics;
		
		tactics = new ArrayList<Tactic>();
		File manifest = files.getRelative("src/android/AndroidManifest.xml");
		File gendir = files.getRelative("src/android/gen");
		File resdir = files.getRelative("src/android/res");
		// I increasing think we should be using "raw" ...
		File assetsDir = files.getRelative("src/android/assets");
		File dexFile = files.getOutput("classes.dex");
		File zipfile = files.getOutput(projectName+".ap_");
		File srcdir = files.getRelative("src/main/java");
		bindir = files.getOutput("classes");
		
		ManifestBuildCommand mbc1 = new ManifestBuildCommand(this, acxt, manifest, true, srcdir, bindir);
		tactics.add(mbc1);
		
		AaptGenBuildCommand gen = new AaptGenBuildCommand(this, acxt, manifest, gendir, resdir);
		tactics.add(gen);
		List<File> genFiles = new DeferredFileList(gendir, "*.java");
		JavaBuildCommand genRes = new JavaBuildCommand(this, files, files.makeRelative(gendir).getPath(), "classes", "gen", genFiles, "android");
		genRes.addToBootClasspath(acxt.getPlatformJar());
		jrr.add(acxt.getPlatformJar());
		tactics.add(genRes);
		List<File> srcFiles;
		if (srcdir.isDirectory()) {
			srcFiles = FileUtils.findFilesMatching(srcdir, "*.java");
			for (int i=0;i<srcFiles.size();)
				if (srcFiles.get(i).getName().startsWith("."))
					srcFiles.remove(i);
				else
					i++;
		} else
			srcFiles = new ArrayList<File>();
		JavaBuildCommand buildSrc = new JavaBuildCommand(this, files, "src/main/java", "classes", "main", srcFiles, "android");
		buildSrc.dontClean();
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		tactics.add(buildSrc);

		ManifestBuildCommand mbc2 = new ManifestBuildCommand(this, acxt, manifest, false, srcdir, bindir);
		tactics.add(mbc2);

		// TODO: I feel it should be possible to compile and run unit tests, but what about that bootclasspath?
		if (files.getRelative("src/test/java").exists())
		{
			JavaBuildCommand buildTests = new JavaBuildCommand(this, files, "src/test/java", "test-classes", "test", FileUtils.findFilesMatching(files.getRelative("src/test/java"), "*.java"), "android");
			buildTests.addToClasspath(new File(files.getOutputDir(), "classes"));
			buildTests.addToBootClasspath(acxt.getPlatformJar());
			tactics.add(buildTests);
			
			buildTests.addToClasspath(files.getRelative("src/main/resources"));
			buildTests.addToClasspath(files.getRelative("src/test/resources"));
			
			JUnitRunCommand junitRun = new JUnitRunCommand(this, files, buildTests);
			junitRun.addToBootClasspath(acxt.getPlatformJar());
			tactics.add(junitRun);
		}
		
		DexBuildCommand dex = new DexBuildCommand(acxt, this, files, files.getOutput("classes"), files.getRelative("src/android/lib"), dexFile, exclusions);
		for (PendingResource pr : uselibs)
		{
			File path = pr.physicalResource().getPath();
			dex.addJar(path);
			jrr.add(path);
		}
		tactics.add(dex);
		AaptPackageBuildCommand pkg = new AaptPackageBuildCommand(this, acxt, manifest, zipfile, resdir, assetsDir);
		tactics.add(pkg);
		ApkBuildCommand apk = new ApkBuildCommand(this, acxt, zipfile, dexFile, apkFile, apkResource);
		tactics.add(apk);
		return tactics;
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
	public ResourcePacket<PendingResource> needsResources() {
		return needs;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return new ResourcePacket<BuildResource>();
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		ResourcePacket<BuildResource> ret = new ResourcePacket<BuildResource>();
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
		return false;
	}

	@Override
	public boolean analyzeExports() {
		return true;
	}

	public void configureJRR(BuildContext cxt) {
		if (jrrConfigured)
			return;
		jrr.add(bindir);
		for (BuildResource br : cxt.getDependencies(this))
		{
			if (br instanceof JarResource)
				jrr.add(((JarResource)br).getPath());
		}
		jrrConfigured = true;
	}
}
