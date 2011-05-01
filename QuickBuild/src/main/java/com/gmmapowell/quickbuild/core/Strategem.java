package com.gmmapowell.quickbuild.core;

import java.io.File;
import java.util.Collection;

import com.gmmapowell.utils.OrderedFileList;

public interface Strategem {
	String identifier();
	ResourcePacket needsResources();
	ResourcePacket providesResources();
	ResourcePacket buildsResources();
	File rootDirectory();
	Collection<? extends Tactic> tactics();
	OrderedFileList sourceFiles();
	boolean onCascade();
}
