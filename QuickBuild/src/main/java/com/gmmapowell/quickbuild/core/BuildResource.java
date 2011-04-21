package com.gmmapowell.quickbuild.core;

import java.io.File;

// TODO: it seems to me that these all need to override equals() and hashCode()
public interface BuildResource {
	Strategem getBuiltBy();
	File getPath();
	String compareAs();
}
