package com.gmmapowell.bytecode;

import com.gmmapowell.bytecode.CPInfo.ClassInfo;

public class ClassPoolEntry {
	private final ClassInfo info;

	public ClassPoolEntry(ClassInfo info) {
		this.info = info;
	}
	
	@Override
	public String toString() {
		return info.toString();
	}

	public String getName() {
		return info.justName();
	}

	public void setName(String rw) {
		info.setName(rw);
	}
}
