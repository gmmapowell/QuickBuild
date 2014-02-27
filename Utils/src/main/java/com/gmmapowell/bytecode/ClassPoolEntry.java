package com.gmmapowell.bytecode;

import java.util.List;

import com.gmmapowell.bytecode.CPInfo.ClassInfo;
import com.gmmapowell.bytecode.CPInfo.NTInfo;
import com.gmmapowell.bytecode.CPInfo.Utf8Info;

public class ClassPoolEntry {
	private final ConstPool pool;
	private final int idx;

	public ClassPoolEntry(ClassInfo info) {
		this.idx = info.idx;
		this.pool = info.pool;
	}

	public ClassPoolEntry(NTInfo info) {
		this.idx = info.descriptor;
		this.pool = info.pool;
	}

	public String getName() {
		return ((Utf8Info)pool.get(idx)).asClean();
	}

	public void setName(String rw) {
		pool.setPoolEntry(idx, new Utf8Info(rw));
	}
	
	public List<String> getReferencedClasses() {
		String sig = ((Utf8Info)pool.get(idx)).asClean();
		List<String> ret;
		if (sig.startsWith("("))
			ret = JavaInfo.unmapSignature(sig, false);
		else
			ret = JavaInfo.simplify(sig);
		return ret;
	}
	
	@Override
	public String toString() {
		return "CPE[" + idx + ":" + pool.get(idx).asClean() + "]";
	}
}
