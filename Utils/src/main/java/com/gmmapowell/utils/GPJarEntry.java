package com.gmmapowell.utils;

import java.io.File;
import java.io.InputStream;
import java.util.jar.JarEntry;
import com.gmmapowell.lambda.FuncR1;

public class GPJarEntry {
	private final GPJarFile jar;
	private final JarEntry entry;
	
	public GPJarEntry(GPJarFile gpJarFile, JarEntry e) {
		this.jar = gpJarFile;
		this.entry = e;
	}

	public String getPackage() {
		return FileUtils.convertToDottedName(new File(entry.getName()).getParentFile());
	}

	public boolean isClassFile() {
		return entry.getName().endsWith(".class");
	}
	
	public String getName() {
		return entry.getName();
	}
	
	public long length() {
		return entry.getSize();
	}
	
	@Override
	public String toString() {
		return entry.toString();
	}

	public static FuncR1<Boolean, GPJarEntry> nameMatches(final String glob) {
		return new FuncR1<Boolean, GPJarEntry>() {
			@Override
			public Boolean apply(GPJarEntry arg1) {
				return StringUtil.globMatch(glob, arg1.getName());
			}
		};
	}

	public InputStream asStream() {
		return jar.getInputStream(entry);
	}
}
