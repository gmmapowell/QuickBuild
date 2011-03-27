package com.gmmapowell.quickbuild.build;

import java.io.File;

public class JarBuildCommand implements BuildCommand {
	private final File projectDir;

	public JarBuildCommand(File projectDir) {
		this.projectDir = projectDir;
		// TODO Auto-generated constructor stub
	}
	
	public void add(File file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean execute(BuildContext cxt) {
		return false;
	}

	@Override
	public String toString() {
		return "Jar Up: " + projectDir;
	}
}
