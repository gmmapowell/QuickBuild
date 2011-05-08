package com.gmmapowell.quickbuild.build.maven;

import java.io.File;

import com.gmmapowell.quickbuild.build.java.JarResource;

public class MavenResource extends JarResource {

	private final String pkgname;

	public MavenResource(String pkgname, File f) {
		super(null, f);
		this.pkgname = pkgname;
	}

	@Override
	public String compareAs() {
		return "MavenJar["+pkgname+"]";
	}
	
	@Override
	public String toString() {
		return "MavenJar["+pkgname+"]";
	}
}
