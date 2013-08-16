package com.gmmapowell.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.gmmapowell.bytecode.CPInfo.Utf8Info;

public class AnnotationArg {
	private final ByteCodeFile bcf;
	final String name;
	final AnnotationValue value;
	
	private AnnotationArg(ByteCodeFile bcf, String name, AnnotationValue value)
	{
		this.bcf = bcf;
		this.name = name;
		this.value = value;
	}
	
	public AnnotationArg(ByteCodeFile bcf, String name, String strValue)
	{
		this.bcf = bcf;
		this.name = name;
		this.value = new AnnotationValue(AnnotationTag.TEXT, strValue);
	}

	public AnnotationArg(ByteCodeFile bcf, String name, String[] paramValue) {
		this.bcf = bcf;
		this.name = name;
		AnnotationValue[] arr = new AnnotationValue[paramValue.length];
		for (int i=0;i<paramValue.length;i++)
			arr[i] = new AnnotationValue(AnnotationTag.TEXT, paramValue[i]); 
		this.value = new AnnotationValue(AnnotationTag.ARRAY, arr);
	}
	
	public static AnnotationArg classParam(ByteCodeFile bcf, String paramName, String className) {
		return new AnnotationArg(bcf, paramName, new AnnotationValue(AnnotationTag.CLASS, bcf.pool.requireUtf8(JavaInfo.map(className))));
	}

	public static AnnotationArg readArg(ByteCodeFile bcf, DataInputStream dis) throws IOException {
		short nameIdx = dis.readShort();
		AnnotationValue value = AnnotationValue.parse(bcf, dis);
//		System.out.println("value = " + value + " name = " + nameIdx);
		return new AnnotationArg(bcf, ((Utf8Info)bcf.pool.get(nameIdx)).asString(), value);
	}

	public static AnnotationArg classArray(ByteCodeFile bcf, String name, String[] classNames)
	{
		AnnotationValue[] classStructs = new AnnotationValue[classNames.length];
		for (int i=0;i<classNames.length;i++)
			classStructs[i] = new AnnotationValue(AnnotationTag.CLASS, bcf.pool.requireUtf8(JavaInfo.map(classNames[i])));
		return new AnnotationArg(bcf, name, new AnnotationValue(AnnotationTag.ARRAY, classStructs));
	}
	
	public static AnnotationArg annArray(ByteCodeFile bcf, String paramName, Annotation[] args) {
		AnnotationValue[] annStructs = new AnnotationValue[args.length];
		for (int i=0;i<args.length;i++)
			annStructs[i] = new AnnotationValue(AnnotationTag.ANNOTATION, args[i]);
		return new AnnotationArg(bcf, paramName, new AnnotationValue(AnnotationTag.ARRAY, annStructs));
	}
	
	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(bcf.pool.requireUtf8(name));
		value.write(bcf, dos);
	}
	
	@Override
	public String toString() {
		return name + "=" + value;
	}
}
