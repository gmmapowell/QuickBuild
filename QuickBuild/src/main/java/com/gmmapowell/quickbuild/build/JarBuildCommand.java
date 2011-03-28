package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gmmapowell.quickbuild.config.Project;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class JarBuildCommand implements BuildCommand {
	private final Project project;
	private final File jarfile;
	private final JarResource jar;
	private final List<File> dirsToJar = new ArrayList<File>();

	public JarBuildCommand(Project project, String jarfile) {
		this.project = project;
		this.jarfile = new File(project.getOutputDir(), jarfile);
		jar = new JarResource(this.jarfile, project);

	}
	
	public void add(File file) {
		dirsToJar.add(file);
	}
	
	public File getFile() {
		return FileUtils.makeRelative(jarfile);
	}

	@Override
	public BuildStatus execute(BuildContext cxt) {
		if (jarfile.exists() && !jarfile.delete())
			throw new QuickBuildException("Could not delete " + jarfile);
		RunProcess proc = new RunProcess("jar");
		proc.captureStdout();
		proc.redirectStderr(System.out);
		proc.arg("cvf");
		proc.arg(jarfile.getPath());
		for (File dir : dirsToJar)
		{
			for (File f : FileUtils.findFilesUnderMatching(dir, "*.class"))
			{
				proc.arg("-C");
				proc.arg(dir.getPath());
				proc.arg(f.getPath());
			}
		}
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			cxt.addBuiltJar(jar);
			return BuildStatus.SUCCESS;
		}
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		return "Jar Up: " + project.getBaseDir();
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public Set<String> getPackagesProvided() {
		return null;
	}

	@Override
	public List<BuildResource> generatedResources() {
		List<BuildResource> ret = new ArrayList<BuildResource>();
		ret.add(jar);
		return ret;
	}
}
