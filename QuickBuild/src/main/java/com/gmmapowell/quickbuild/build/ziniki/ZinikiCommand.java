package com.gmmapowell.quickbuild.build.ziniki;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.JUnitLibCommand;
import com.gmmapowell.quickbuild.build.java.JUnitRunCommand;
import com.gmmapowell.quickbuild.build.java.JavaBuildCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategem;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class ZinikiCommand extends AbstractStrategem {
	private String projectName;
	private final File rootdir;
	private String javaVersion;
	private String mode;
	private final List<PendingResource> junitLibs = new ArrayList<PendingResource>();

	public ZinikiCommand(TokenizedLine toks) {
		super(toks, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public void addChild(ConfigApplyCommand opt) {
		if (opt instanceof ZinikiModeCommand) {
			if (mode != null)
				throw new UtilException("Cannot specify more than one mode");
			mode = ((ZinikiModeCommand)opt).getMode();
		}
		else if (opt instanceof JUnitLibCommand)
		{
			junitLibs.add(((JUnitLibCommand)opt).getResource());
		}
		else
			throw new UtilException("Cannot handle command " + opt);
	}

	@Override
	public Strategem applyConfig(Config config) {
		javaVersion = config.getVarIfDefined("javaVersion", null);
		File pmz = config.getPath("pmziniki");
		StructureHelper files = new StructureHelper(rootdir, config.getOutput());

		// Generate Ziniki proj jar
		ZinikiGenerateCommand gen = new ZinikiGenerateCommand(this, pmz, projectName.toLowerCase());
		gen.builds(gen.getResource());
		tactics.add(gen);
		
		// Build all the Java files
		List<File> srcFiles = FileUtils.findFilesMatching(new File(rootdir, "src/main/java"), "*.java");
		JavaBuildCommand jbc = null;
		if (srcFiles != null && !srcFiles.isEmpty()) {
			jbc = new JavaBuildCommand(this, files, "src/main/java", "classes", "main", srcFiles, "jdk", javaVersion, true);
			jbc.needs(new PendingResource(gen.getResource()));
			jbc.addProcessDependency(gen);
			tactics.add(jbc);
		}

		// Test it 
		List<File> testFiles = FileUtils.findFilesMatching(new File(rootdir, "src/test/java"), "*.java");
		if (testFiles != null && !testFiles.isEmpty()) {
			JavaBuildCommand juc = new JavaBuildCommand(this, files, "src/test/java", "test-classes", "test", testFiles, "jdk", javaVersion, true);
			juc.needs(new PendingResource(gen.getResource()));
			juc.addProcessDependency(gen);
			if (jbc != null) {
				juc.addProcessDependency(jbc);
				juc.addToClasspath(new File(files.getOutputDir(), "classes"));
			}
			tactics.add(juc);
			
			{
				JUnitRunCommand jur = new JUnitRunCommand(this, files, juc);
				jur.addLibs(junitLibs);
				jur.addProcessDependency(juc);
				tactics.add(jur);
			}
		}
		
		// Create the deploy archinve
		ZinikiDeployCommand deploy = new ZinikiDeployCommand(this, pmz, projectName.toLowerCase());
		if (mode != null)
			deploy.setMode("--"+mode);
		deploy.builds(deploy.getResource());
		deploy.addProcessDependency(gen);
		if (jbc != null)
			deploy.addProcessDependency(jbc);
		tactics.add(deploy);
		return this;
	}

	@Override
	public String identifier() {
		return "ZinikiCommand[" + rootdir + "]";
	}

	@Override
	public File rootDirectory() {
		return rootdir;
	}

	@Override
	public boolean onCascade() {
		// TODO Auto-generated method stub
		return false;
	}
}
