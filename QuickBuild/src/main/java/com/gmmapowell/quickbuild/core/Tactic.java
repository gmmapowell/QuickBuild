package com.gmmapowell.quickbuild.core;

import java.util.Set;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import org.zinutils.utils.OrderedFileList;

public interface Tactic {

	public Strategem belongsTo();

	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug);

	public String identifier();
	
	ResourcePacket<PendingResource> needsResources();
	ResourcePacket<BuildResource> providesResources();
	ResourcePacket<BuildResource> buildsResources();
	OrderedFileList sourceFiles();

	public void addProcessDependency(Tactic earlier);

	Set<Tactic> getProcessDependencies();

	public boolean analyzeExports();
}
