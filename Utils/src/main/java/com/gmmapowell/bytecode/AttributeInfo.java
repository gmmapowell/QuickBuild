package com.gmmapowell.bytecode;

import com.gmmapowell.bytecode.CPInfo.Utf8Info;

public class AttributeInfo {
	private final ConstPool pool;
	final int nameIdx;
	private final byte[] bytes;

	public AttributeInfo(ConstPool pool, int nameIdx, byte[] bytes) {
		this.pool = pool;
		this.nameIdx = nameIdx;
		this.bytes = bytes;
	}

	public AttributeInfo(ByteCodeFile bcf, String attrClass, byte[] bytes) {
		pool = bcf.pool;
		nameIdx = pool.requireUtf8(attrClass);
		this.bytes = bytes;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public boolean hasName(String s)
	{
		return s.equals(((Utf8Info)pool.get(nameIdx)).asString());
	}
	
	@Override
	public String toString() {
		return "AttrInfo[" + pool.get(nameIdx) + ":" + bytes.length + "]";
	}
}
