package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.DeferredFileList;
import com.gmmapowell.quickbuild.build.java.JarBuildCommand;
import com.gmmapowell.quickbuild.build.java.JavaBuildCommand;
import com.gmmapowell.quickbuild.build.java.JavaVersionCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;
import org.zinutils.utils.FileUtils;

public class AndroidJarCommand extends AbstractStrategem {
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();
	private String projectName;
	private String targetName;
	private final File projectDir;
	private AndroidContext acxt;
	private StructureHelper files;
	private File srcdir;
	private String javaVersion;

	public AndroidJarCommand(TokenizedLine toks) {
		super(toks, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}
	
	@Override
	public AndroidJarCommand applyConfig(Config config) {
		acxt = config.getAndroidContext();
		files = new StructureHelper(projectDir, config.getOutput());
		targetName = projectName + ".jar";
		srcdir = files.getRelative("src/main/java");
		javaVersion = config.getVarIfDefined("javaVersion", null);
		for (ConfigApplyCommand cmd : options)
		{
			cmd.applyTo(config);
			if (cmd instanceof JavaVersionCommand)
			{
				javaVersion = ((JavaVersionCommand)cmd).getVersion();
			}
			else
				throw new UtilException("Cannot handle " + cmd);
		}
		createTactics();
		return this;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	public void createTactics() {
		// Hasten, hasten ... cutten and pasten from AndroidCommand
		File manifest = files.getRelative("src/android/AndroidManifest.xml");
		File gendir = files.getRelative("src/android/gen");
		File resdir = files.getRelative("src/android/res");
		if (resdir.exists())
		{
			AaptGenBuildCommand gen = new AaptGenBuildCommand(this, acxt, manifest, gendir, resdir);
			tactics.add(gen);
			List<File> genFiles = new DeferredFileList(gendir, "*.java");
			JavaBuildCommand genRes = new JavaBuildCommand(this, files, files.makeRelative(gendir).getPath(), "classes", "gen", genFiles, "android", javaVersion, true);
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
		JavaBuildCommand buildSrc = new JavaBuildCommand(this, files, "src/main/java", "classes", "main", FileUtils.findFilesMatching(files.getRelative("src/main/java"), "*.java"), "android", javaVersion, false);
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
		JarBuildCommand jar = new JarBuildCommand(this, files, targetName, null, null, null);
		jar.add(files.getOutput("classes"));
		if (resdir.exists())
			jar.add(resdir);
		tactics.add(jar);
	}

	@Override
	public String toString() {
		return "AndroidJar " + projectName;
	}

	@Override
	public File rootDirectory() {
		return projectDir;
	}

	@Override
	public String identifier() {
		return "AndroidJar[" + targetName + "]";
	}

	@Override
	public boolean onCascade() {
		return false;
	}
}
