package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.zinutils.bytecode.JavaRuntimeReplica;
import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.DeferredFileList;
import com.gmmapowell.quickbuild.build.java.ExcludeCommand;
import com.gmmapowell.quickbuild.build.java.JUnitLibCommand;
import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.build.java.JarBuildCommand;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.build.java.JavaBuildCommand;
import com.gmmapowell.quickbuild.build.java.JavaNature;
import com.gmmapowell.quickbuild.build.java.JavaVersionCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategem;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;

public class AndroidCommand extends AbstractStrategem {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String projectName;
	private final File rootDir;
	private AndroidContext acxt;
	private StructureHelper files;
	private ApkResource apkResource;
	private boolean useJack;
	private File apkFile;
	private ResourcePacket<PendingResource> uselibs = new ResourcePacket<PendingResource>();
	private ResourcePacket<PendingResource> usejni = new ResourcePacket<PendingResource>();
	private ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final List<BuildResource> junitLibs = new ArrayList<BuildResource>();
	private Set<Pattern> exclusions = new HashSet<Pattern>();
	final JavaRuntimeReplica jrr;
	private File bindir;
	private ApkBuildCommand apkTactic;
	private String javaVersion;
	private File keystorePath;
	private AndroidRestrictJNICommand jniRestrict;
	private AndroidEspressoTestsCommand espressoTests;
	private String exportJar;

	public AndroidCommand(TokenizedLine toks) {
		super(toks,
				new ArgumentDefinition("--jack", Cardinality.OPTION, "useJack", "use new JACK/JILL buildchain"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootDir = FileUtils.findDirectoryNamed(projectName);
		this.jrr = new JavaRuntimeReplica();
	}
	
	public void addToJRR(File f) {
		this.jrr.add(f);
	}

	@Override
	public Strategem applyConfig(Config config) {
		config.getNature(JavaNature.class);
		config.getNature(AndroidNature.class);
		files = new StructureHelper(rootDir, config.getOutput());
		FileUtils.assertDirectory(files.getRelative("src/android/res"));
		FileUtils.assertDirectory(files.getRelative("src/android/lib"));
		acxt = config.getAndroidContext();
		apkFile = files.getOutput(projectName+".apk");
		javaVersion = config.getVarIfDefined("javaVersion", null);
		keystorePath = config.getPath("androidKeystore");

		for (ConfigApplyCommand cmd : options)
		{
			cmd.applyTo(config);
			if (cmd instanceof AndroidUseLibraryCommand)
			{
				PendingResource pr = ((AndroidUseLibraryCommand)cmd).getResource();
				uselibs.add(pr);
				needs.add(pr);
			}
			else if (cmd instanceof AndroidUseJNICommand)
			{
				PendingResource pr = ((AndroidUseJNICommand)cmd).getResource();
				usejni.add(pr);
				needs.add(pr);
			}
			else if (cmd instanceof ExcludeCommand)
			{
				exclusions.add(((ExcludeCommand) cmd).getPattern());
			}
			else if (cmd instanceof JavaVersionCommand)
			{
				javaVersion = ((JavaVersionCommand)cmd).getVersion();
			}
			else if (cmd instanceof AndroidRestrictJNICommand)
			{
				if (jniRestrict != null)
					throw new UtilException("Cannot specify more than one JNI restriction");
				jniRestrict = (AndroidRestrictJNICommand)cmd;
			}
			else if (cmd instanceof AndroidEspressoTestsCommand) {
				espressoTests = (AndroidEspressoTestsCommand)cmd;
				needs.add(espressoTests.getResource());
			}
			else if (cmd instanceof AndroidExportJarCommand) {
				exportJar = projectName + ".jar";
			}
			else if (cmd instanceof JUnitLibCommand)
			{
				addJUnitLib((JUnitLibCommand)cmd);
			}
			else
				throw new UtilException("Cannot handle " + cmd);
		}

		createTactics(); // ensure they're generated
		apkResource = apkTactic.getResource();
		
		return this;
	}
	
	private void addJUnitLib(JUnitLibCommand opt) {
		junitLibs.add(opt.getResource());
	}


	public void createTactics() {
		File manifest = files.getRelative("src/android/AndroidManifest.xml");
		File gendir = files.getRelative("src/android/gen");
		File resdir = files.getRelative("src/android/res");
		// I increasing think we should be using "raw" ...
		File assetsDir = files.getRelative("src/android/assets");
		File rawDir = files.getRelative("src/android/rawapk");
		File dexFile = files.getOutput("classes.dex");
		File dexDir = files.getOutput("dex");
		File jillDir = files.getOutput("jacks");
		File zipfile = files.getOutput(projectName+".ap_");
		File srcdir = files.getRelative("src/main/java");
		bindir = files.getOutput("classes");
		
		ManifestBuildCommand mbc1 = null;
		if (espressoTests == null) {
			mbc1 = new ManifestBuildCommand(this, acxt, manifest, true, srcdir, bindir);
			tactics.add(mbc1);
		}
		
		AaptGenBuildCommand gen = new AaptGenBuildCommand(this, acxt, manifest, gendir, resdir);
		tactics.add(gen);
		if (espressoTests == null) {
			gen.addProcessDependency(mbc1);
		}
		List<File> genFiles = new DeferredFileList(gendir, "*.java");
		JavaBuildCommand genRes = new JavaBuildCommand(this, files, files.makeRelative(gendir).getPath(), "classes", "gen", genFiles, "android", javaVersion, true);
		genRes.dontClean();
		for (PendingResource pr : needs)
			genRes.needs(pr);
		genRes.addToBootClasspath(acxt.getPlatformJar());
		jrr.add(acxt.getPlatformJar());
		if (espressoTests == null) {
			genRes.addToBootClasspath(acxt.getSupportJar());
			jrr.add(acxt.getSupportJar());
		}
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

		JavaBuildCommand buildSrc = new JavaBuildCommand(this, files, "src/main/java", "classes", "main", srcFiles, "android", javaVersion, true);
		buildSrc.dontClean();
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		if (espressoTests == null)
			buildSrc.addToBootClasspath(acxt.getSupportJar());
		tactics.add(buildSrc);
		buildSrc.addProcessDependency(gen);

		Tactic prior = buildSrc;
		if (espressoTests == null) {
			ManifestBuildCommand mbc2 = new ManifestBuildCommand(this, acxt, manifest, false, srcdir, bindir);
			tactics.add(mbc2);
			mbc2.addProcessDependency(buildSrc);
			prior = mbc2;
		}

		if (files.getRelative("src/test/java").exists())
		{
			List<File> testSources = FileUtils.findFilesMatching(files.getRelative("src/test/java"), "*.java");
			if (testSources.size() > 0)
			{
				JavaBuildCommand buildTests = new JavaBuildCommand(this, files, "src/test/java", "test-classes", "test", testSources, "android", javaVersion, false);
				buildTests.dontClean();
				buildTests.addToClasspath(new File(files.getOutputDir(), "classes"));
				buildTests.addToBootClasspath(acxt.getPlatformJar());
				buildTests.addToBootClasspath(acxt.getSupportJar());
				buildTests.addToClasspath(files.getRelative("src/main/resources"));
				buildTests.addToClasspath(files.getRelative("src/test/resources"));
				tactics.add(buildTests);
				buildTests.addProcessDependency(prior);
				
				
				JUnitRunCommand junitRun = new JUnitRunCommand(this, files, buildTests, null, null);
				junitRun.addToClasspath(acxt.getPlatformJar());
				junitRun.addToClasspath(acxt.getSupportJar());
				junitRun.addLibs(junitLibs);
				tactics.add(junitRun);
				junitRun.addProcessDependency(buildTests);
				
				// I don't think that running the tests is actually a dependency for the later steps
//				prior = junitRun;
			}
		}
		
		if (espressoTests != null && files.getRelative("src/espresso/java").exists())
		{
			List<File> espressoSources = FileUtils.findFilesMatching(files.getRelative("src/espresso/java"), "*.java");
			if (espressoSources.size() > 0)
			{
				JavaBuildCommand buildTests = new JavaBuildCommand(this, files, "src/espresso/java", "classes", "espresso", espressoSources, "android", javaVersion, false);
				buildTests.dontClean();
				buildTests.addResource(espressoTests.getResource());
				buildTests.addToClasspath(new File(files.getOutputDir(), "classes"));
				buildTests.addToBootClasspath(acxt.getPlatformJar());
				buildTests.addToBootClasspath(acxt.getSupportJar());
				tactics.add(buildTests);
				buildTests.addProcessDependency(prior);
				
				prior = buildTests;
			}
		}
		
		if (exportJar != null) {
			JarBuildCommand jbc = new JarBuildCommand(this, files, exportJar, null, null, null, null);
			
			JarResource jarResource = jbc.getJarResource();
			if (jarResource != null && buildSrc != null)
				jbc.builds(jarResource);

			jbc.add(files.getOutput("classes"));
			if (resdir.exists())
				jbc.add(resdir);

			tactics.add(jbc);
			jbc.addProcessDependency(prior);
			prior = jbc;
		}
		
		if (espressoTests != null) {
			exclusions.add(Pattern.compile(acxt.getSupportJar().getName()));
		}
		
		Tactic assembleTactic;
		if (useJack) {
			JackBuildCommand jack = new JackBuildCommand(acxt, this, files, files.getOutput("classes"), files.getRelative("src/android/lib"), dexDir, jillDir, exclusions, uselibs);
			tactics.add(jack);
			jack.addProcessDependency(prior);
			assembleTactic = jack;
			dexFile = new File(dexDir, "classes.dex");
		} else {
			DexBuildCommand dex = new DexBuildCommand(acxt, this, files, files.getOutput("classes"), files.getRelative("src/android/lib"), dexFile, exclusions, uselibs);
			for (PendingResource pr : uselibs)
			{
				try
				{
					File path = pr.physicalResource().getPath();
					jrr.add(path);
				}
				catch (Exception ex)
				{
					System.out.println("Could not add " + pr + " to jrr path because it did not exist: " + ex.getMessage());
				}
			}
			if (jniRestrict != null)
				dex.restrictArch(jniRestrict.arch);
			tactics.add(dex);
			dex.addProcessDependency(prior);
			assembleTactic = dex;
		}
		
		AaptPackageBuildCommand pkg = new AaptPackageBuildCommand(this, acxt, manifest, zipfile, resdir, assetsDir, rawDir);
		tactics.add(pkg);
		pkg.addProcessDependency(assembleTactic);
		
		apkTactic = new ApkBuildCommand(this, acxt, zipfile, dexFile, keystorePath, apkFile);
		apkTactic.builds(apkTactic.apkResource);
		tactics.add(apkTactic);
		apkTactic.addProcessDependency(pkg);
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
	public File rootDirectory() {
		return rootDir;
	}

	@Override
	public String identifier() {
		return "BuildApk[" + apkResource.compareAs() +"]";
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	public void configureJRR(BuildContext cxt) {
		jrr.add(bindir);
		for (Tactic t : tactics())
			for (BuildResource br : cxt.getDependencies(t))
			{
				if (br instanceof JarResource)
				{
					jrr.add(((JarResource)br).getPath());
				}
			}
	}
	
}
