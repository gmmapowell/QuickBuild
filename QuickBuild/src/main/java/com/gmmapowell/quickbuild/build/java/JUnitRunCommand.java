package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.List;

import com.gmmapowell.bytecode.ByteCodeFile;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.DependencyFloat;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JUnitRunCommand implements Tactic, DependencyFloat {
	private final File srcdir;
	private File bindir;
	
	// TODO: this is currently unused ... it should be, I think, for Android
	private final BuildClassPath bootclasspath = new BuildClassPath();
	private final Strategem parent;
	private final JavaBuildCommand jbc;
	private ResourcePacket addlResources;


	public JUnitRunCommand(Strategem parent, StructureHelper files, JavaBuildCommand jbc) {
		this.parent = parent;
		this.jbc = jbc;
		this.srcdir = new File(files.getBaseDir(), "src/test/java");
		this.bindir = files.getOutput("test-classes");
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		RunClassPath classpath = new RunClassPath(jbc);
		if (addlResources != null)
			for (BuildResource r : addlResources)
			{
				classpath.add(cxt.getPendingResource((PendingResource) r).getPath());
			}
		RunProcess proc = new RunProcess("java");
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();

		// TODO: use bootclasspath
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		proc.arg("org.junit.runner.JUnitCore");
		boolean any = false;
		for (File f : FileUtils.findFilesUnderMatching(srcdir, "*.java"))
		{
			String qualifiedName = FileUtils.convertToDottedNameDroppingExtension(f);
			File clsFile = new File(bindir, FileUtils.ensureExtension(f, ".class").getPath());
			System.out.println(clsFile);
			ByteCodeFile bcf = new ByteCodeFile(clsFile, qualifiedName);
			if (bcf.hasMethodsWithAnnotation("org.junit.Test"))
			{
				any = true;
				proc.arg(qualifiedName);
			}
		}
		if (!any)
			return BuildStatus.SKIPPED;
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			return BuildStatus.SUCCESS;
		}
		System.out.println(" !! JUnit Test Errors will be presented at end");
		cxt.junitFailure(this, proc.getStdout(), proc.getStderr());
		return BuildStatus.TEST_FAILURES;
	}

	@Override
	public String toString() {
		return "JUnit Runner: " + srcdir;
	}
	
	public void addToBootClasspath(File resource) {
		bootclasspath.add(resource);
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	public void addLibs(List<PendingResource> junitLibs) {
		if (junitLibs.isEmpty())
			return;
		
		addlResources = new ResourcePacket();
		for (PendingResource r : junitLibs)
			addlResources.add(r);
	}

	@Override
	public ResourcePacket needsAdditionalBuiltResources() {
		return addlResources;
	}
}
