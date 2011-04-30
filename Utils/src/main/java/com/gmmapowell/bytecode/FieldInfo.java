package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FieldInfo extends JavaInfo {
	private final ByteCodeFile bcf;
	private final int access_flags;
	private final short name_idx;
	private final short descriptor_idx;
	private final List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();

	public FieldInfo(ByteCodeFile bcf, boolean isFinal, String type, String var) {
		this.bcf = bcf;
		int flags = ByteCodeFile.ACC_PRIVATE;
		if (isFinal)
			flags |= ByteCodeFile.ACC_FINAL;
		access_flags = flags;
		this.name_idx = bcf.requireUtf8(var);
		this.descriptor_idx = bcf.requireUtf8(map(type));
	}

	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(access_flags);
		dos.writeShort(name_idx);
		dos.writeShort(descriptor_idx);
		bcf.writeAttributes(dos, attributes);
	}

}
