package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildContext;
import org.zinutils.utils.PathBuilder;

public class RunClassPath extends BuildClassPath {
	private final BuildClassPath basedOn;
	private final File utilsJar;
	
	public RunClassPath(BuildContext cxt, JavaBuildCommand jbc)
	{
		if (jbc == null)
			basedOn = new BuildClassPath();
		else
			basedOn = jbc.getClassPath();
		utilsJar = cxt.getUtilsJar();
	}
	
	@Override
	public void add(File file) {
		if (file == null || basedOn.contains(file))
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
