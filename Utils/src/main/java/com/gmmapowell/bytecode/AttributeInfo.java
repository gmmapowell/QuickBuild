package com.gmmapowell.bytecode;

import com.gmmapowell.bytecode.CPInfo.Utf8Info;

public class AttributeInfo {
	private final CPInfo[] pool;
	private final int nameIdx;
	private final byte[] bytes;

	public AttributeInfo(CPInfo[] pool, int nameIdx, byte[] bytes) {
		this.pool = pool;
		this.nameIdx = nameIdx;
		this.bytes = bytes;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public boolean hasName(String s)
	{
		return s.equals(((Utf8Info)pool[nameIdx]).asString());
	}
	
	@Override
	public String toString() {
		return "AttrInfo[" + pool[nameIdx] + ":" + bytes.length + "]";
	}
}
