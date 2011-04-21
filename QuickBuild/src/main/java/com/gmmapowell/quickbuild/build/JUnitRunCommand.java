package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JUnitRunCommand implements Tactic {
	private final File srcdir;
	private final RunClassPath classpath;
	private final BuildClassPath bootclasspath = new BuildClassPath();
	private final StructureHelper files;

	public JUnitRunCommand(Strategem parent, StructureHelper files, JavaBuildCommand jbc) {
		this.files = files;
		this.srcdir = new File(files.getBaseDir(), "src/test/java");
		classpath = new RunClassPath(jbc);
	}

	@Override
	public BuildStatus execute(BuildContext cxt) {
		// TODO: cxt.addAllProjectDirs(classpath, project);
		RunProcess proc = new RunProcess("java");
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
		System.out.println("FAIL!!! JUnit Errors will be presented at end");
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
		// TODO Auto-generated method stub
		return null;
	}


}
