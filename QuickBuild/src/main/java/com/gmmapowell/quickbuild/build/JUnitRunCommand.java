package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JUnitRunCommand implements BuildCommand {
	private final Project project;
	private final File srcdir;
	private final RunClassPath classpath;
	private final BuildClassPath bootclasspath = new BuildClassPath();

	public JUnitRunCommand(Project project, JavaBuildCommand jbc) {
		this.project = project;
		this.srcdir = new File(project.getBaseDir(), "src/test/java");
		classpath = new RunClassPath(jbc);
	}

	@Override
	public BuildStatus execute(BuildContext cxt) {
		cxt.addAllProjectDirs(classpath, project);
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
	public Project getProject() {
		return project;
	}
	
	@Override
	public String toString() {
		return "JUnit Runner: " + srcdir;
	}

	@Override
	public Set<String> getPackagesProvided() {
		return null;
	}

	@Override
	public List<BuildResource> generatedResources() {
		// TODO Auto-generated method stub
		return null;
	}

	public void addToBootClasspath(File resource) {
		bootclasspath.add(resource);
	}


}
