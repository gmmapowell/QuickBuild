package com.gmmapowell.quickbuild.build.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.build.java.JarResource;

public class MavenResource extends JarResource {
	private final MavenNature nature;
	private final String pkgname;

	public MavenResource(MavenNature nature, String pkgname, File f) {
		super(null, f);
		this.nature = nature;
		this.pkgname = pkgname;
	}

	@Override
	public List<File> getPaths() {
		ArrayList<File> list = new ArrayList<File>();
		list.add(file);
		File platform = nature.makePlatformPath(file);
		if (platform.canRead() && platform.length() > 0)
			list.add(platform);
		return list;
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
