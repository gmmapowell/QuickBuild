package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Annotation
{
	private final ByteCodeFile bcf;
	final String name;
	private final List<AnnotationArg> args = new ArrayList<AnnotationArg>();
	private final int param;

	public Annotation(ByteCodeFile bcf, String name) {
		this(bcf, name, -1);
	}

	public Annotation(ByteCodeFile bcf, String name, int param) {
		this.bcf = bcf;
		this.name = name;
		this.param = param;
	}

	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(bcf.requireUtf8(JavaInfo.map(name))); 
		dos.writeShort(args.size());
		for (AnnotationArg a : args)
			a.write(dos);
	}

	public void addParam(String paramName, String paramValue) {
		args.add(new AnnotationArg(bcf, paramName, paramValue));
	}
	
	public void addParam(String paramName, String[] paramValue) {
		args.add(new AnnotationArg(bcf, paramName, paramValue));
	}
	
	public void addClassParam(String paramName, String className) {
		args.add(AnnotationArg.classParam(bcf, paramName, className));
	}
	
	public void addClassParams(String paramName, String... classes) {
		args.add(AnnotationArg.classArray(bcf, paramName, classes));
	}
	
	public void addAnnParam(String paramName, Annotation... args) {
		this.args.add(AnnotationArg.annArray(bcf, paramName, args));
	}

	public int forParam() {
		return param;
	}
}