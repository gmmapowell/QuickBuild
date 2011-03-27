package com.gmmapowell.quickbuild.build;

import com.gmmapowell.utils.PathBuilder;

public class RunClassPath extends BuildClassPath {
	private final BuildClassPath basedOn;
	
	public RunClassPath(JavaBuildCommand jbc)
	{
		basedOn = jbc.getClassPath();
	}
	
	public String toString()
	{
		PathBuilder pb = new PathBuilder();
		this.toString(pb);
		basedOn.toString(pb);
		return pb.toString();
	}

}
