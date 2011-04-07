package com.gmmapowell.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.lambda.FuncR1;
import com.gmmapowell.lambda.Lambda;
import com.gmmapowell.utils.FileUtils;

public class MethodCreator extends MethodInfo {
	private final List<String> arguments = new ArrayList<String>();
	private String returnType = "V";
	protected final List<Instruction> instructions = new ArrayList<Instruction>();
	private int opdepth = 0;
	protected int locals = 0;
	protected int maxStack = 0;
	private FuncR1<String, String> mapType = new FuncR1<String, String>() {
		@Override
		public String apply(String arg1) {
			return map(arg1);
		}
	};
	private final ByteCodeCreator byteCodeCreator;
	private final String name;

	public MethodCreator(ByteCodeCreator byteCodeCreator, ByteCodeFile bcf, boolean isStatic, String name) {
		super(bcf);
		this.byteCodeCreator = byteCodeCreator;
		this.name = name;
		nameIdx = bcf.requireUtf8(name);
		if (!isStatic)
			locals++;
	}

	public int argument(String type) {
		int ret = locals++;
		arguments.add(map(type));
		return ret;
	}
	
	public void complete() {
		if (access_flags == -1)
			access_flags = ByteCodeFile.ACC_PUBLIC;
		if (instructions.size() == 0)
			access_flags |= ByteCodeFile.ACC_ABSTRACT;
		else
		{
			if (opdepth != 0)
				throw new UtilException("Stack was left with depth non-zero");
			if (instructions.size() > 0)
			{
				int hdrlen = 2 + 2 + 4 /* + len */ + 2 /* + exc */ + 2 /* + attrs */;
				int len = 0;
				for (Instruction i : instructions)
					len += i.length();
				try
				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					dos.writeShort(bcf.requireUtf8("Code"));
					dos.writeInt(hdrlen + len);
					dos.writeShort(maxStack);
					dos.writeShort(locals);
					dos.writeInt(len);
					for (Instruction i : instructions)
						i.write(dos);
					dos.writeShort(0); // exceptions
					dos.writeShort(0); // code attributes
					attributes.add(new AttributeInfo(baos.toByteArray()));
				}
				catch (Exception ex)
				{
					throw UtilException.wrap(ex);
				}
			}
		}
		descriptorIdx = bcf.requireUtf8(signature());
	}

	private String signature() {
		return signature(returnType, arguments);
	}

	private String signature(String ret, Iterable<String> args) {
		StringBuilder sb = new StringBuilder("(");
		for (String s : args)
			sb.append(s);
		sb.append(")");
		sb.append(ret);
		return sb.toString();
	}

	private String map(String type) {
		if (type.startsWith("@")) // this is my own annotation to allow pre-mapped types to be passed around
			return type.substring(1);
		int dims = 0;
		while (type.charAt(dims) == '[')
			dims++;
		return type.substring(0, dims) + mapScalar(type.substring(dims));
	}
	
	private String mapScalar(String type)
	{
		if (type.equals("void"))
			return "V";
		else if (type.equals("int"))
			return "I";
		else if (type.equals("byte"))
			return "B";
		else if (type.equals("char"))
			return "C";
		else if (type.equals("double"))
			return "D";
		else if (type.equals("float"))
			return "F";
		else if (type.equals("long"))
			return "J";
		else if (type.equals("short"))
			return "S";
		else if (type.equals("boolean"))
			return "Z";
		return "L"+FileUtils.convertDottedToSlashPath(type) +";";
	}

	private void opstack(int i) {
		opdepth += i;
		if (i > maxStack)
			maxStack = opdepth;
		System.out.println("Opdepth = " + opdepth);
	}
	
	public void aload(int i) {
		if (i < 4)
			instructions.add(new Instruction(0x2a+i));
		else
			instructions.add(new Instruction(0x19, i));
		opstack(1);
	}
	
	public void invokeparent(String... args) {
		invokespecial(byteCodeCreator.getSuperClass(), "@" + returnType, name, args);
	}

	private void invokespecial(String clz, String ret, String meth, String... args) {
		int clzIdx = bcf.requireClass(clz);
		int methIdx = bcf.requireUtf8(meth);
		int sigIdx = bcf.requireUtf8(signature(map(ret), Lambda.map(mapType, CollectionUtils.listOf(args))));
		int ntIdx = bcf.requireNT(methIdx, sigIdx);
		int idx = bcf.requireRef(ByteCodeFile.CONSTANT_Methodref, clzIdx, ntIdx);
		instructions.add(new Instruction(0xb7, (idx>>8)&0xff, (idx&0xff)));
		opstack(-args.length-1);
	}

	public void returnVoid() {
		instructions.add(new Instruction(0xb1));
	}
}
