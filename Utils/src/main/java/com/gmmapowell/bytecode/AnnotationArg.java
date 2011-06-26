package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public class AnnotationArg {
	private final ByteCodeFile bcf;
	final String name;
	final AnnotationTag tag;
	final Object value;
	
	public AnnotationArg(ByteCodeFile bcf, String name, String strValue)
	{
		this.bcf = bcf;
		this.name = name;
		this.tag = AnnotationTag.TEXT;
		this.value = strValue;
	}

	public AnnotationArg(ByteCodeFile bcf, String name, String[] paramValue) {
		this.bcf = bcf;
		this.name = name;
		this.tag = AnnotationTag.ARRAY;
		AnnotationArg[] arr = new AnnotationArg[paramValue.length];
		this.value = arr;
		for (int i=0;i<paramValue.length;i++)
			arr[i] = new AnnotationArg(bcf, null, paramValue[i]); 
	}

	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(bcf.requireUtf8(name));
		tag.write(bcf, dos, value);
	}
}