package com.gmmapowell.quickbuild.build.ziniki;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.build.java.BuildClassPath;
import com.gmmapowell.quickbuild.build.java.JarResource;
import com.gmmapowell.quickbuild.core.AbstractTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class ZinikiDeployCommand extends AbstractTactic {
	private final File pmzPath;
	private BuildResource jarResource;
	private String mode = null;

	public ZinikiDeployCommand(Strategem parent, File pmzPath) {
		super(parent);
		this.pmzPath = pmzPath;
		this.jarResource = new JarResource(this, new File(parent.rootDirectory(), "gen/chat-proj.jar"));
	}

	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList(FileUtils.findFilesMatching(new File(parent.rootDirectory(), "src/main/resources"), "*.xml"));
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		BuildClassPath classpath = new BuildClassPath();
		for (File f : FileUtils.findFilesMatching(new File(pmzPath, "root"), "*.jar"))
			classpath.add(f);
		for (File f : FileUtils.findFilesMatching(new File(pmzPath, "libs"), "*.jar"))
			classpath.add(f);
		RunProcess proc = new RunProcess("java");
		proc.executeInDir(parent.rootDirectory());
		proc.captureStdout();
		proc.captureStderr();
		proc.showArgs(showArgs);
		proc.showArgs(true);
		proc.debug(showDebug);
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		proc.arg("org.ziniki.tools.deploy.ZinikiDeploy");
		if (mode != null)
			proc.arg(mode);
		proc.arg("--bindir");
		proc.arg("qbout/classes");
		proc.arg("-o");
		proc.arg("chat.jar");
		proc.arg(".");
		proc.arg("--reference");
		proc.arg(new File(pmzPath, "builtins/builtin.jar").getPath());
		proc.arg("--reference");
		proc.arg(new File(pmzPath, "privileged/datamodel.jar").getPath());
		proc.execute();
		if (proc.getExitCode() == 0) {
			cxt.builtResource(jarResource, false);
			return BuildStatus.SUCCESS;
		}
		cxt.output.buildErrors(proc.getStdout());
		return BuildStatus.BROKEN;
	}

	@Override
	public String identifier() {
		return "ZinikiDeploy["+parent.rootDirectory()+"]";
	}

	@Override
	public String toString() {
		return "Deploy Ziniki[" + FileUtils.makeRelative(parent.rootDirectory()) + "]";
	}

	public BuildResource getResource() {
		return jarResource;
	}
}
