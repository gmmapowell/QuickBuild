package com.gmmapowell.bytecode;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.bytecode.CPInfo.Utf8Info;
import com.gmmapowell.exceptions.UtilException;


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
	
	public Annotation(ByteCodeFile bcf, String name, List<AnnotationArg> args) {
		this.bcf = bcf;
		this.name = name;
		this.args.addAll(args);
		this.param = -1;
	}

	public static List<Annotation> parse(ByteCodeFile bcf, AttributeInfo attr) {
		byte[] bs = attr.getBytes();
//		System.out.println("Hex: " + StringUtil.hex(bs));
//		System.out.print("Txt: ");
//		for (int i=0;i<bs.length;i++)
//		{
//			if (Character.isISOControl(bs[i]))
//				System.out.print("  ");
//			else
//				System.out.print(" " + (char)bs[i]);
//		}
//		System.out.println();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bs));
		return parse(bcf, dis);
	}

	public static List<Annotation> parse(ByteCodeFile bcf, DataInputStream dis) {
		try
		{
			List<Annotation> ret = new ArrayList<Annotation>();
			short cnt = dis.readShort();
			for (int i=0;i<cnt;i++)
			{
				short idx = dis.readShort();
				String name = JavaInfo.unmap(((Utf8Info)bcf.pool[idx]).asString());
				int argcnt = dis.readShort();
				List<AnnotationArg> args = new ArrayList<AnnotationArg>();
				for (int j=0;j<argcnt;j++)
					args.add(AnnotationArg.readArg(bcf, dis));
				ret.add(new Annotation(bcf, name, args));
			}
			return ret;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
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
	
	@Override
	public String toString() {
		return "Annotation["+name+":"+args+"]";
	}

	public AnnotationValue getArg(String string) {
		for (AnnotationArg a : args)
		{
			if (a.name.equals(string))
				return a.value;
		}
		return null;
	}
}