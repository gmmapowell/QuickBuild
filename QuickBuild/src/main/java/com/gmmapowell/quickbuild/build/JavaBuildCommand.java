package com.gmmapowell.quickbuild.build;

import java.io.File;

import com.gmmapowell.system.RunProcess;

public class JavaBuildCommand implements BuildCommand {
	private final File projectDir;
	private final String src;
	private final String bin;

	public JavaBuildCommand(File projectDir, String src, String bin) {
		this.projectDir = projectDir;
		this.src = src;
		this.bin = bin;
	}

	@Override
	public void execute(BuildContext cxt) {
		RunProcess proc = new RunProcess("java");
		proc.redirectStdout(System.out);
		proc.redirectStderr(System.out);
		proc.execute();
	}

	@Override
	public String toString() {
		return "Java Compile: " + src;
	}
}
