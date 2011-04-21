package com.gmmapowell.quickbuild.build.java;

public class JUnitFailure {

	private final JUnitRunCommand cmd;
	private final String stdout;
	private final String stderr;

	public JUnitFailure(JUnitRunCommand cmd, String stdout, String stderr) {
		this.cmd = cmd;
		this.stdout = stdout;
		this.stderr = stderr;
	}

	public void show() {
		System.out.println("=============");
		System.out.println(cmd + " failed:");
		System.out.println("Stdout:");
		System.out.println(stdout);
		System.out.println("Stderr:");
		System.out.println(stderr);
		System.out.println("=============");
		System.out.println();
	}

}
