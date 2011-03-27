package com.gmmapowell.quickbuild.build;

import java.io.File;

public class MavenResource extends JarResource {

	private final String pkgname;

	public MavenResource(String pkgname, File f) {
		super(f);
		this.pkgname = pkgname;
	}

	@Override
	public String toString() {
		return "MavenJar["+pkgname+"]";
	}
}
