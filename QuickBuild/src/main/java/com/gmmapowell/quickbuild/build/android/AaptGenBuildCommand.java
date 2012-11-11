package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildOrder;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.system.RunProcess;
import com.gmmapowell.utils.FileUtils;

public class AaptGenBuildCommand implements Tactic {

	private final AndroidContext acxt;
	private final File gendir;
	private final File manifestFile;
	private final File resdir;
	private final Strategem parent;

	public AaptGenBuildCommand(Strategem parent, AndroidContext acxt, File manifest, File gendir, File resdir) {
		this.parent = parent;
		this.acxt = acxt;
		this.gendir = gendir;
		this.manifestFile = manifest;
		this.resdir = resdir;
	}
	
	/* TODO: you can get into a world of hurt if you duplicate IDs.  Android lets you do this, because it makes sense
	 * across different layout types (e.g. landscape).  But if you literally end up with two in the same view, it's really bad
	 * (you lose windows and the like).
	 * 
	 * We should add a check and at least a warning for this here:
	 * 
	 * On the command line I do this:
	 */ 
	 // grep +id src/android/res/layout*/* | sed 's/src\/android\/res\/\(layout-*[^/]*\)\/.*+id\/\(.*\)".*/\1 \2/' | sort | uniq -c | grep -v '^ *1 '
	/*
	 * which searches through all the layout files to find lines that include "+id" - creating a new id
	 * for each of these, it figures out the directory (\1) and the id (\2) by splitting a line like:
	 * 
	 * src/android/res/layout/main.xml:	android:id="@+id/newId"
	 *
	 * into "layout/main" and "newId"
	 * 
	 * It then sorts them and applies a uniqueness filter to the pair.  Note that this is neither necessary or sufficient, but it's a good catch given
	 * I just spent two hours trying to track down an issue caused by this and there are no obvious symptoms.
	 */

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		FileUtils.assertDirectory(gendir);
		FileUtils.cleanDirectory(gendir);
		RunProcess proc = new RunProcess(acxt.getAAPT().getPath());
		proc.showArgs(showArgs);
		proc.debug(showDebug);
		proc.captureStdout();
		proc.captureStderr();
		
		proc.arg("package");
		proc.arg("-m");
		proc.arg("-J");
		proc.arg(gendir.getPath());
		proc.arg("-M");
		proc.arg(manifestFile.getPath());
		proc.arg("-S");
		proc.arg(resdir.getPath());
		proc.arg("-I");
		proc.arg(acxt.getPlatformJar().getPath());
		proc.execute();
		if (proc.getExitCode() == 0)
		{
			return BuildStatus.SUCCESS;
		}
		System.out.println(proc.getStderr());
		return BuildStatus.BROKEN;
	}

	@Override
	public String toString() {
		return "aapt gen: " + FileUtils.makeRelative(gendir);
	}

	@Override
	public Strategem belongsTo() {
		return parent;
	}

	@Override
	public String identifier() {
		return BuildOrder.tacticIdentifier(parent, "aaptgen");
	}

	private Set <Tactic> procDeps = new HashSet<Tactic>();
	
	@Override
	public void addProcessDependency(Tactic earlier) {
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}
}
