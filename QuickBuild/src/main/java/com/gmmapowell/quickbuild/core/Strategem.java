package com.gmmapowell.quickbuild.core;

import java.io.File;
import java.util.Collection;

import com.gmmapowell.utils.OrderedFileList;

public interface Strategem {
	String identifier();
	ResourcePacket<PendingResource> needsResources();
	ResourcePacket<BuildResource> providesResources();
	ResourcePacket<BuildResource> buildsResources();
	File rootDirectory();
	Collection<? extends Tactic> tactics();
	OrderedFileList sourceFiles();
	boolean onCascade();
}
