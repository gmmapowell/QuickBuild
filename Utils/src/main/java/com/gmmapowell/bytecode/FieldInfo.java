package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class FieldInfo extends JavaInfo {
	private final ByteCodeFile bcf;
	private int access_flags;
	private short name_idx;
	private short descriptor_idx;
	final List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();

	public FieldInfo(ByteCodeFile bcf, boolean isFinal, Access access, String type, String var) {
		this.bcf = bcf;
		int flags = 0;
		switch (access)
		{
		case PRIVATE:
			flags = ByteCodeFile.ACC_PRIVATE;
			break;
		case PROTECTED:
			flags = ByteCodeFile.ACC_PROTECTED;
			break;
		case DEFAULT:
			break;
		case PUBLIC:
			flags = ByteCodeFile.ACC_PUBLIC;
			break;
		default:
			throw new UtilException("Invalid access");
		}
		if (isFinal)
			flags |= ByteCodeFile.ACC_FINAL;
		access_flags = flags;
		this.name_idx = bcf.requireUtf8(var);
		this.descriptor_idx = bcf.requireUtf8(map(type));
	}

	public FieldInfo(ByteCodeFile bcf) {
		this.bcf = bcf;
	}

	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(access_flags);
		dos.writeShort(name_idx);
		dos.writeShort(descriptor_idx);
		bcf.writeAttributes(dos, attributes);
	}

}
