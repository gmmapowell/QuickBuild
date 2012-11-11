package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.utils.PathBuilder;

public class RunClassPath extends BuildClassPath {
	private final BuildClassPath basedOn;
	private final File utilsJar;
	
	public RunClassPath(BuildContext cxt, JavaBuildCommand jbc)
	{
		basedOn = jbc.getClassPath();
		utilsJar = cxt.getUtilsJar();
	}
	
	@Override
	public void add(File file) {
		if (basedOn.contains(file))
			return;
		super.add(file);
	}
	
	public String toString()
	{
		PathBuilder pb = new PathBuilder();
		this.toString(pb);
		basedOn.toString(pb);
		pb.add(utilsJar);
		return pb.toString();
	}

}
