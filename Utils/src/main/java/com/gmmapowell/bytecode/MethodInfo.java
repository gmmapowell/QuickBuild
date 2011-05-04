package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MethodInfo extends JavaInfo {
	protected short access_flags = -1;
	protected short nameIdx = -1;
	protected short descriptorIdx = -1;
	protected final ByteCodeFile bcf;
	protected final List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();

	public MethodInfo(ByteCodeFile bcf)
	{
		this.bcf = bcf;
	}
	
	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(access_flags);
		dos.writeShort(nameIdx);
		dos.writeShort(descriptorIdx);
		bcf.writeAttributes(dos, attributes);
	}

	@Override
	public String toString() {
		return "Method[" + bcf.pool[nameIdx] +"]";
	}
}