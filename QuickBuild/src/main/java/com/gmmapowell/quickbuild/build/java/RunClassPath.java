package com.gmmapowell.quickbuild.build.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.quickbuild.build.BuildContext;
import org.zinutils.utils.PathBuilder;

public class RunClassPath extends BuildClassPath {
	private final List<BuildClassPath> cpl = new ArrayList<BuildClassPath>();
	private final File utilsJar;
	
	public RunClassPath(BuildContext cxt, List<JavaBuildCommand> testBuilds) {
		if (testBuilds != null) {
			for (JavaBuildCommand jbc : testBuilds)
				cpl.add(jbc.getClassPath());
		}
		utilsJar = cxt.getUtilsJar();
	}

	@Override
	public void add(File file) {
		if (file == null)
			return;
		for (BuildClassPath cp : cpl) {
			if (cp.contains(file))
				return;
		}
		super.add(file);
	}
	
	public String toString()
	{
		PathBuilder pb = new PathBuilder();
		this.toString(pb);
		for (BuildClassPath bp : cpl)
			bp.toString(pb);
		pb.add(utilsJar);
		return pb.toString();
	}
}
