package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.DeferredFileList;
import com.gmmapowell.quickbuild.build.java.JarBuildCommand;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.build.java.JavaBuildCommand;
import com.gmmapowell.quickbuild.build.java.JavaSourceDirResource;
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


public class AndroidJarCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem {
	private String projectName;
	private String targetName;
	private final File projectDir;
	private AndroidContext acxt;
	private StructureHelper files;
	private File srcdir;
	private JarResource androidJar;
	private List<Tactic> tactics;

	@SuppressWarnings("unchecked")
	public AndroidJarCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}
	
	@Override
	public AndroidJarCommand applyConfig(Config config) {
		acxt = config.getAndroidContext();
		files = new StructureHelper(projectDir, config.getOutput());
		targetName = projectName + ".jar";
		srcdir = files.getRelative("src/main/java");
		androidJar = new JarResource(this, new File(files.getOutputDir(), targetName));
		return this;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		// I don't think we need this at the moment
		throw new UtilException("Cannot handle " + obj);
	}

	@Override
	public List<? extends Tactic> tactics() {
		if (tactics != null)
			return tactics;
		tactics = new ArrayList<Tactic>();
		
		// Hasten, hasten ... cutten and pasten from AndroidCommand
		File manifest = files.getRelative("src/android/AndroidManifest.xml");
		File gendir = files.getRelative("src/android/gen");
		File resdir = files.getRelative("src/android/res");
		if (resdir.exists())
		{
			AaptGenBuildCommand gen = new AaptGenBuildCommand(this, acxt, manifest, gendir, resdir);
			tactics.add(gen);
			List<File> genFiles = new DeferredFileList(gendir, "*.java");
			JavaBuildCommand genRes = new JavaBuildCommand(this, files, files.makeRelative(gendir).getPath(), "classes", "gen", genFiles, "android");
			genRes.addToBootClasspath(acxt.getPlatformJar());
//			jrr.add(acxt.getPlatformJar());
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
		}
		JavaBuildCommand buildSrc = new JavaBuildCommand(this, files, "src/main/java", "classes", "main", FileUtils.findFilesMatching(files.getRelative("src/main/java"), "*.java"), "android");
		buildSrc.dontClean();
		buildSrc.addToBootClasspath(acxt.getPlatformJar());
		tactics.add(buildSrc);
		
		/* I think this is a bad idea ...
		 * It's not so much a bad idea, as by definition this isn't using the JDK ...
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
		JarBuildCommand jar = new JarBuildCommand(this, files, androidJar, null, null);
		jar.add(files.getOutput("classes"));
		if (resdir.exists())
			jar.add(resdir);
		tactics.add(jar);

		return tactics;
	}

	@Override
	public String toString() {
		return "AndroidJar " + projectName;
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return new ResourcePacket<PendingResource>();
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		ResourcePacket<BuildResource> ret = new ResourcePacket<BuildResource>();
		ret.add(new JavaSourceDirResource(this, srcdir, FileUtils.findFilesMatching(files.getRelative("src/main/java"), "*.java")));
		return ret;
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		ResourcePacket<BuildResource> ret = new ResourcePacket<BuildResource>();
		ret.add(androidJar);
		return ret;
	}

	@Override
	public File rootDirectory() {
		return projectDir;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList(projectDir, "*.java");
	}

	@Override
	public String identifier() {
		return "AndroidJar[" + targetName + "]";
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public boolean analyzeExports() {
		return true;
	}
}
