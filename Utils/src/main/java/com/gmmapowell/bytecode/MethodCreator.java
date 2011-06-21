package com.gmmapowell.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.lambda.FuncR1;
import com.gmmapowell.lambda.Lambda;

public class MethodCreator extends MethodInfo {
	private final List<String> arguments = new ArrayList<String>();
	private String returnType;
	protected final List<Instruction> instructions = new ArrayList<Instruction>();
	private int opdepth = 0;
	protected int locals = 0;
	protected int maxStack = 0;
	protected boolean lenientMode = false;
	private FuncR1<String, String> mapType = new FuncR1<String, String>() {
		@Override
		public String apply(String arg1) {
			return map(arg1);
		}
	};
	private final ByteCodeCreator byteCodeCreator;
	private final String name;
	private List<String> exceptions = new ArrayList<String>();

	public MethodCreator(ByteCodeCreator byteCodeCreator, ByteCodeFile bcf, boolean isStatic, String returnType, String name) {
		super(bcf);
		this.name = name;
		this.returnType = map(returnType);
		this.byteCodeCreator = byteCodeCreator;
		nameIdx = bcf.requireUtf8(name);
		if (!isStatic)
		{
			locals = 1;
		}
	}

	public void setAccess(Access a)
	{
		if (a == Access.PUBLIC)
			access_flags = ByteCodeFile.ACC_PUBLIC;
		else if (a == Access.PRIVATE)
			access_flags = ByteCodeFile.ACC_PRIVATE;
		else if (a == Access.PROTECTED)
			access_flags = ByteCodeFile.ACC_PROTECTED;
		else
			throw new UtilException("Huh?");
	}
	
	public void addAttribute(String named, String text) {
		short ptr = bcf.requireUtf8(text);
		byte[] data = new byte[2];
		data[0] = (byte)(ptr>>8);
		data[1] = (byte)(ptr&0xff);
		attributes.add(bcf.newAttribute(named, data));
	}

	public void lenientMode(boolean mode)
	{
		if (mode)
			System.err.println("Turning on lenient mode for " + bcf + " method " + name);
		this.lenientMode = mode;
	}
	
	public int argument(String type) {
		int ret = locals++;
		arguments.add(map(type));
		return ret;
	}

	public void throwsException(String exception) {
		exceptions.add(exception);
	}

	public void complete() {
		if (access_flags == -1)
			access_flags = ByteCodeFile.ACC_PUBLIC;
		if (exceptions.size() != 0)
		{
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeShort(exceptions.size());
				for (String s : exceptions)
					dos.writeShort(bcf.requireClass(s));
				attributes.add(bcf.newAttribute("Exceptions", baos.toByteArray()));
			}
			catch (Exception ex)
			{
				throw UtilException.wrap(ex);
			}
		}
		if (instructions.size() == 0)
			access_flags |= ByteCodeFile.ACC_ABSTRACT;
		else
		{
			if (opdepth != 0)
			{
				if (lenientMode)
					System.err.println("Stack was left with depth " + opdepth + " after processing " + name + " for " + bcf);
				else
					throw new UtilException("Stack was left with depth " + opdepth + " after processing " + name + " for " + bcf);
			}
			if (instructions.size() > 0)
			{
				int len = 0;
				for (Instruction i : instructions)
					len += i.length();
				try
				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					dos.writeShort(maxStack);
					dos.writeShort(locals);
					dos.writeInt(len);
					for (Instruction i : instructions)
						i.write(dos);
					dos.writeShort(0); // exceptions
					dos.writeShort(0); // code attributes
					attributes.add(bcf.newAttribute("Code", baos.toByteArray()));
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

	public static String signature(String ret, Iterable<String> args) {
		StringBuilder sb = new StringBuilder("(");
		for (String s : args)
			sb.append(s);
		sb.append(")");
		sb.append(ret);
		return sb.toString();
	}

	private int hi(int idx) {
		return (idx>>8)&0xff;
	}

	private int lo(int idx) {
		return (idx&0xff);
	}

	private void add(int stackChange, Instruction instruction) {
		instructions.add(instruction);
		opstack(stackChange);
		if (lenientMode)
			System.err.println(instruction + " stack = " + opdepth);
	}

	private void opstack(int i) {
		opdepth += i;
		if (opdepth < 0)
			throw new UtilException("Stack underflow generating " + name + " in " + bcf);
		if (opdepth > maxStack)
			maxStack = opdepth;
	}
	
	public void aconst_null() {
		add(1, new Instruction(0x1));
	}
	
	public void aload(int i) {
		if (i < 4)
			add(1, new Instruction(0x2a+i));
		else
			add(1, new Instruction(0x19, i));
	}
	
	public void areturn() {
		add(-1, new Instruction(0xb0));
	}

	public void astore(int i) {
		if (i < 4)
			add(-1, new Instruction(0x4b+i));
		else
			add(-1, new Instruction(0x3a, i));
		if (i >= locals)
			locals = i+1;
	}
	
	public void athrow() {
		add(-1, new Instruction(0xbf));
	}
	
	public void checkCast(String clz)
	{
		int idx = bcf.requireClass(clz);
		add(0, new Instruction(0xc0, idx>>8, idx &0xff));
	}

	public void dup() {
		add(1, new Instruction(0x59));
	}

	public void getField(String clz, String type, String var) {
		int clzIdx = bcf.requireClass(clz);
		int fieldIdx = bcf.requireUtf8(var);
		int sigIdx = bcf.requireUtf8(map(type));
		int ntIdx = bcf.requireNT(fieldIdx, sigIdx);
		int idx = bcf.requireRef(ByteCodeFile.CONSTANT_Fieldref, clzIdx, ntIdx);
		add(0, new Instruction(0xb4, hi(idx), lo(idx)));
	}

	public Marker ifeq() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0x99, 00, 00));
		return ret;
	}
	
	public Marker ifnull() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0xc6, 00, 00));
		return ret;
	}

	public void invokeOtherConstructor(String clz,	String... args) {
		invoke(0xb7, clz, "void", "<init>", args);
	}
	
	public void invokeParentConstructor(String... args) {
		if (byteCodeCreator.getSuperClass() == null)
			throw new UtilException("Cannot use parent methods without defining superclass");
		invoke(0xb7, byteCodeCreator.getSuperClass(), "void", "<init>", args);
	}

	public void invokeParentMethod(String typeReturn, String method, String... args) {
		if (byteCodeCreator.getSuperClass() == null)
			throw new UtilException("Cannot use parent methods without defining superclass");
		invoke(0xb7, byteCodeCreator.getSuperClass(), typeReturn, method, args);
	}

	private int invokeIdx(String clz, String ret, String meth, String... args) {
		int clzIdx = bcf.requireClass(clz);
		int methIdx = bcf.requireUtf8(meth);
		int sigIdx = bcf.requireUtf8(signature(map(ret), Lambda.map(mapType, CollectionUtils.listOf(args))));
		int ntIdx = bcf.requireNT(methIdx, sigIdx);
		int idx = bcf.requireRef(ByteCodeFile.CONSTANT_Methodref, clzIdx, ntIdx);
		return idx;
	}
	
	private void addInvoke(Instruction instruction, String ret, String... args) {
		int pop = args.length;
		if (ret.equals("void"))
			++pop;
		add(-pop, instruction);
	}
	
	private void invoke(int opcode, String clz, String ret, String meth, String... args) {
		int idx = invokeIdx(clz, ret, meth, args);
		addInvoke(new Instruction(opcode, hi(idx), lo(idx)), ret, args);
	}

	public void invokeVirtualMethod(String clz, String ret, String method, String... args) {
		invoke(0xb6, clz, ret, method, args);
	}

	public void invokeInterface(String clz, String ret, String method, String... args) {
		int idx = invokeIdx(clz, ret, method, args);
		int count = args.length+1;
		// TODO: double and long values should add to count
		addInvoke(new Instruction(0xb9, hi(idx), lo(idx), count, 0), ret, args);
	}

	public void ldcClass(String clz)
	{
		add(1, new Instruction(0x12, bcf.requireClass(clz)));
	}

	public void ldcString(String string) {
		add(1, new Instruction(0x12, bcf.requireString(string)));
	}
	
	public void newObject(String clz) {
		int idx = bcf.requireClass(clz);
		add(1, new Instruction(0xbb, idx>>8,idx&0xff));
	}

	public void pop() {
		add(-1, new Instruction(0x57));
	}

	public void putField(String clz, String type, String var) {
		int clzIdx = bcf.requireClass(clz);
		int fieldIdx = bcf.requireUtf8(var);
		int sigIdx = bcf.requireUtf8(map(type));
		int ntIdx = bcf.requireNT(fieldIdx, sigIdx);
		int idx = bcf.requireRef(ByteCodeFile.CONSTANT_Fieldref, clzIdx, ntIdx);
		add(-2, new Instruction(0xb5, idx>>8, idx&0xff));
	}

	public void returnVoid() {
		add(0, new Instruction(0xb1));
	}
}
