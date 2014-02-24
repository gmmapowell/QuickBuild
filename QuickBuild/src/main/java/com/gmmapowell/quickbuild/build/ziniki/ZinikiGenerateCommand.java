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

public class ZinikiGenerateCommand extends AbstractTactic {
	private final File pmzPath;
	private BuildResource jarResource;

	public ZinikiGenerateCommand(Strategem parent, File pmzPath) {
		super(parent);
		this.pmzPath = pmzPath;
		this.jarResource = new JarResource(this, new File(parent.rootDirectory(), "gen/chat-proj.jar"));
	}

	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList(FileUtils.findFilesMatching(new File(parent.rootDirectory(), "src/main/resources"), "*.xml"));
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
//		proc.showArgs(true);
		proc.debug(showDebug);
		proc.arg("-classpath");
		proc.arg(classpath.toString());
		proc.arg("org.ziniki.tools.generator.ZinikiGenerator");
		proc.arg("-o");
		proc.arg("gen/chat-proj.jar");
		proc.arg(".");
		proc.arg("--reference");
		proc.arg(new File(pmzPath, "builtins/builtin.jar").getPath());
		proc.arg("--reference");
		proc.arg(new File(pmzPath, "privileged/datamodel.jar").getPath());
		proc.execute();
		if (proc.getExitCode() == 0) {
			cxt.builtResource(jarResource);
			return BuildStatus.SUCCESS;
		}
		cxt.output.buildErrors(proc.getStdout() + proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String identifier() {
		return "ZinikiGen["+parent.rootDirectory()+"]";
	}

	@Override
	public String toString() {
		return "Generate Ziniki[" + FileUtils.makeRelative(parent.rootDirectory()) + "]";
	}

	public BuildResource getResource() {
		return jarResource;
	}
}
