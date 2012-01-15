package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import com.gmmapowell.bytecode.JavaInfo.Access;

public class InnerClass implements Comparable<InnerClass> {
	private final Access access;
	private final int inner;
	private final int enclosing;
	private final int innerName;

	public InnerClass(ByteCodeFile bcf, Access access, String projClz, String enclosingClass, String innerName) {
		this.access = access;
		inner = bcf.requireClass(projClz);
		enclosing = bcf.requireClass(enclosingClass);
		this.innerName = bcf.requireUtf8(innerName);
	}

	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(inner);
		dos.writeShort(enclosing);
		dos.writeShort(innerName);
		dos.writeShort(access.asShort());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InnerClass))
			return false;
		InnerClass other = (InnerClass)obj;
		return inner == other.inner && enclosing == other.enclosing;
	}
	
	@Override
	public int hashCode() {
		return inner * enclosing;
	}

	@Override
	public int compareTo(InnerClass o) {
		if (enclosing < o.enclosing)
			return -1;
		if (enclosing > o.enclosing)
			return 1;
		if (inner < o.inner)
			return -1;
		if (inner > o.inner)
			return 1;
		return 0;
	}
}
