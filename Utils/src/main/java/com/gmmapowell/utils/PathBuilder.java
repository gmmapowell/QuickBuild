package com.gmmapowell.utils;

import java.io.File;

public class PathBuilder {
	private StringBuilder sb = new StringBuilder();
	
	public void add(File f) {
		if (f == null)
			return;
		if (f.exists() && (f.length() > 0 || f.isDirectory()))
		{
			if (sb.length() > 0)
				sb.append(File.pathSeparatorChar);
			sb.append(f.getPath());
		}
	}
	
	@Override
	public String toString() {
		if (sb.length() == 0)
			return "\"\"";
		return sb.toString();
	}
}
