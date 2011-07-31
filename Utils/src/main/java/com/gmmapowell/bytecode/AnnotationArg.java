package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public class AnnotationArg {
	private final ByteCodeFile bcf;
	final String name;
	final AnnotationTag tag;
	final Object value;
	
	private AnnotationArg(ByteCodeFile bcf, String name, AnnotationTag tag, Object value)
	{
		this.bcf = bcf;
		this.name = name;
		this.tag = tag;
		this.value = value;
	}
	
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
	
	public static AnnotationArg classParam(ByteCodeFile bcf, String paramName, String className) {
		return new AnnotationArg(bcf, paramName, AnnotationTag.CLASS, bcf.requireUtf8(JavaInfo.map(className)));
	}

	public static AnnotationArg classArray(ByteCodeFile bcf, String name, String[] classNames)
	{
		AnnotationArg[] classStructs = new AnnotationArg[classNames.length];
		for (int i=0;i<classNames.length;i++)
			classStructs[i] = new AnnotationArg(bcf, null, AnnotationTag.CLASS, bcf.requireUtf8(JavaInfo.map(classNames[i])));
		return new AnnotationArg(bcf, name, AnnotationTag.ARRAY, classStructs);
	}
	
	public static AnnotationArg annArray(ByteCodeFile bcf, String paramName, Annotation[] args) {
		AnnotationArg[] annStructs = new AnnotationArg[args.length];
		for (int i=0;i<args.length;i++)
			annStructs[i] = new AnnotationArg(bcf, null, AnnotationTag.ANNOTATION, args[i]);
		return new AnnotationArg(bcf, paramName, AnnotationTag.ARRAY, annStructs);
	}
	
	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(bcf.requireUtf8(name));
		tag.write(bcf, dos, value);
	}
}