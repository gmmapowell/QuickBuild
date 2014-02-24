package com.gmmapowell.quickbuild.build.ziniki;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.JarResource;
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

	public ZinikiCommand(TokenizedLine toks) {
		super(toks, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"));
		rootdir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		if (obj instanceof ZinikiModeCommand) {
			if (mode != null)
				throw new UtilException("Cannot specify more than one mode");
			mode = ((ZinikiModeCommand)obj).getMode();
		} else
			throw new UtilException("Cannot handle command " + obj);
	}

	@Override
	public Strategem applyConfig(Config config) {
		javaVersion = config.getVarIfDefined("javaVersion", null);
		File pmz = config.getPath("pmziniki");
		StructureHelper files = new StructureHelper(rootdir, config.getOutput());

		// Generate Ziniki proj jar
		ZinikiGenerateCommand gen = new ZinikiGenerateCommand(this, pmz);
		gen.builds(gen.getResource());
		tactics.add(gen);
		
		// Build all the Java files
		JavaBuildCommand jbc = new JavaBuildCommand(this, files, "src/main/java", "classes", "main", FileUtils.findFilesMatching(new File(rootdir, "src/main/java"), "*.java"), "jdk", javaVersion, true);
		jbc.needs(new PendingResource(gen.getResource()));
		jbc.addProcessDependency(gen);
		tactics.add(jbc);
		
		// Create the deploy archinve
		ZinikiDeployCommand deploy = new ZinikiDeployCommand(this, pmz);
		if (mode != null)
			deploy.setMode("--"+mode);
//		deploy.builds(deploy.getResource());
		jbc.addProcessDependency(gen);
		jbc.addProcessDependency(jbc);
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
