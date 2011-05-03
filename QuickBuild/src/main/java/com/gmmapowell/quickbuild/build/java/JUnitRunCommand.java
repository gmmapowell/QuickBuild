package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JUnitRunCommand implements Tactic {
	private final File srcdir;
	
	// TODO: this is currently unused ... it should be, I think, for Android
	private final BuildClassPath bootclasspath = new BuildClassPath();
	private final Strategem parent;
	private final List<PendingResource> deferred = new ArrayList<PendingResource>();
	private final JavaBuildCommand jbc;

	public JUnitRunCommand(Strategem parent, StructureHelper files, JavaBuildCommand jbc) {
		this.parent = parent;
		this.jbc = jbc;
		this.srcdir = new File(files.getBaseDir(), "src/test/java");
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		RunClassPath classpath = new RunClassPath(jbc);
		for (PendingResource r : deferred)
		{
			classpath.add(cxt.getPendingResource(r).getPath());
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
			// TODO: check whether it has any @Test annotations
			any = true;
			proc.arg(FileUtils.convertToDottedNameDroppingExtension(f));
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
	
	public void addToClasspath(PendingResource r) {
		deferred.add(r);
	}

	public void addToBootClasspath(File resource) {
		bootclasspath.add(resource);
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}


}
