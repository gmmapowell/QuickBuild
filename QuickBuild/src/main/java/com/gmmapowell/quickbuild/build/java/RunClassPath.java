package com.gmmapowell.quickbuild.build.java;

import java.io.File;

import com.gmmapowell.quickbuild.build.BuildContext;
import org.zinutils.utils.PathBuilder;

public class RunClassPath extends BuildClassPath {
	private final BuildClassPath tcp;
	private final BuildClassPath gcp;
	private final File utilsJar;
	
	public RunClassPath(BuildContext cxt, JavaBuildCommand jbc, JavaBuildCommand jgbc)
	{
		if (jbc == null)
			tcp = new BuildClassPath();
		else
			tcp = jbc.getClassPath();
		if (jgbc == null)
			gcp = new BuildClassPath();
		else
			gcp = jgbc.getClassPath();
		utilsJar = cxt.getUtilsJar();
	}
	
	@Override
	public void add(File file) {
		if (file == null || tcp.contains(file) || gcp.contains(file))
			return;
		super.add(file);
	}
	
	public String toString()
	{
		PathBuilder pb = new PathBuilder();
		this.toString(pb);
		tcp.toString(pb);
		gcp.toString(pb);
		pb.add(utilsJar);
		return pb.toString();
	}

}
