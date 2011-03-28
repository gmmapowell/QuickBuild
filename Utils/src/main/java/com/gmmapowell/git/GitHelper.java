package com.gmmapowell.git;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class GitHelper {

	// TODO: this also needs to have a method which takes a file which was the set of hash values for last time
	// and re-run git hash-object on all the files to see if any of them have changed.
	// There's also the need to write such a list out when the build completes.
	// This may in fact replace everything that's here.
	
	
	public static Set<File> dirtyProjects(Set<File> keySet) {
		HashSet<File> ret = new HashSet<File>();
		RunProcess git = new RunProcess("git");
		git.executeInDir(FileUtils.getCurrentDir());
		git.captureStdout();
		git.arg("status");
		git.execute();
		if (git.getExitCode() != 0)
			throw new UtilException("git returned non-zero error code");
		
		LinePatternParser lpp = new LinePatternParser();
		lpp.match("modified:\\s*(.*)", "modified", "filename");
		lpp.matchAll("#\\s*(\\S+)", "untracked", "filename");
		for (LinePatternMatch lpm : lpp.applyTo(git.stdoutReader()))
		{
//			System.out.println(lpm);
			for (File p : keySet)
			{
				String path = FileUtils.makeRelative(p).getPath();
				if (lpm.get("filename").startsWith(path))
				{
//					System.out.println("Adding " + p);
					ret.add(p);
				}
			}
		}
		return ret;
	}

}
