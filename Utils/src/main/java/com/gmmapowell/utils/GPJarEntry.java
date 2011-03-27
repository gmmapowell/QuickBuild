package com.gmmapowell.utils;

import java.io.File;
import java.util.jar.JarEntry;

public class GPJarEntry {
	private final JarEntry entry;
	
	public GPJarEntry(JarEntry e) {
		this.entry = e;
	}

	public String getPackage() {
		return FileUtils.convertToDottedName(new File(entry.getName()).getParentFile());
	}

	public boolean isClassFile() {
		return entry.getName().endsWith(".class");
	}

}
