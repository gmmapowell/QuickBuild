package com.gmmapowell.quickbuild.core;

import java.io.File;
import java.util.Collection;

public interface Strategem {
	ResourcePacket needsResources();
	ResourcePacket providesResources();
	ResourcePacket buildsResources();
	File rootDirectory();
	Collection<? extends Tactic> tactics();
}
