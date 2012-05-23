package com.gmmapowell.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.bytecode.IfExpr.IfCond;
import com.gmmapowell.bytecode.Var.AVar;
import com.gmmapowell.bytecode.Var.DVar;
import com.gmmapowell.bytecode.Var.IVar;
import com.gmmapowell.bytecode.Var.LVar;
import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.collections.ListMap;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.lambda.FuncR1;
import com.gmmapowell.lambda.Lambda;

public class MethodCreator extends MethodInfo implements MethodDefiner {
	private final List<Var> arguments = new ArrayList<Var>();
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
	private final ListMap<AnnotationType, Annotation> annotations = new ListMap<AnnotationType, Annotation>(AnnotationType.sortOrder);
	private final boolean isStatic;
	private boolean doCount = true;

	public MethodCreator(ByteCodeCreator byteCodeCreator, ByteCodeFile bcf, boolean isStatic, String returnType, String name) {
		super(bcf);
		this.isStatic = isStatic;
		this.name = name;
		this.returnType = map(returnType);
		this.byteCodeCreator = byteCodeCreator;
		nameIdx = bcf.pool.requireUtf8(name);
		if (!isStatic)
		{
			locals = 1;
		}
	}

	@Override
	public void setAccess(Access a)
	{
		access_flags = a.asShort();
	}

	@Override
	public void makeFinal() {
		access_flags |= ByteCodeFile.ACC_FINAL;
	}
	
	@Override
	public void addAttribute(String named, String text) {
		short ptr = bcf.pool.requireUtf8(text);
		byte[] data = new byte[2];
		data[0] = (byte)(ptr>>8);
		data[1] = (byte)(ptr&0xff);
		attributes.add(bcf.newAttribute(named, data));
	}

	@Override
	public void lenientMode(boolean mode)
	{
		if (mode)
			System.err.println("Turning on lenient mode for " + bcf + " method " + name);
		this.lenientMode = mode;
	}
	
	@Override
	public Var argument(String type, String aname) {
		Var ret = varOfType(type, aname);
		ret.setArgument(arguments.size());
		arguments.add(ret);
		return ret;
	}

	// @Deprecated // I would like to deprecate this, but can't pay the cost right now
	// Use argument(type, name) instead ...
	@Override
	public Var argument(String type) {
		return argument(type, "arg" + (arguments.size()-1));
	}

	@Override
	public Var getArgument(int i) {
		return arguments.get(i);
	}

	@Override
	public Var varOfType(String type, String aname) {
		// NOTE: internally, the JVM uses integers for booleans, so I'm copying that
		if (type.equals("int") || type.equals("boolean"))
			return ivar(type, aname);
		else if (type.equals("long"))
			return lvar(type, name);
		else if (type.equals("double"))
			return dvar(type, aname);
		else
			return avar(type, aname);
	}
	
	@Override
	public AVar myThis() {
		if (isStatic)
			throw new UtilException("Static methods don't have 'this'");
		return AVar.myThis(this);
	}

	@Override
	public Var saveAslocal(String clz, String name) {
		Var ret = varOfType(clz, name);
		ret.store();
		return ret;
	}


	@Override
	public Var avar(JavaType clz, String name) {
		return new AVar(this, clz, name);
	}

	@Override
	public Var avar(String clz, String name)
	{
		return new AVar(this, clz, name);
	}
	
	@Override
	public Var ivar(String clz, String name)
	{
		return new IVar(this, clz, name);
	}
	
	@Override
	public Var lvar(String clz, String name)
	{
		return new LVar(this, clz, name);
	}

	@Override
	public Var dvar(String clz, String name)
	{
		return new DVar(this, clz, name);
	}
	
	@Override
	public Expr aNull() {
		return new NullExpr(this);
	}

	@Override
	public int argCount() {
		return arguments.size();
	}

	// TODO: this shouldn't be so hard.  We should have a "field object" that
	// we can ask for its "getter"
	@Override
	@Deprecated
	public FieldExpr field(Expr from, String clz, String type, String named) {
		return new FieldExpr(this, from, clz, type, named);
	}

	@Override
	public FieldExpr getField(String name) {
		return byteCodeCreator.getField(this, name);
	}

	@Override
	public FieldExpr getField(Expr on, String name) {
		return byteCodeCreator.getField(this, on, name);
	}

	@Override
	public Expr staticField(String clz, String type, String named) {
		return new FieldExpr(this, null, clz, type, named);
	}

	@Override
	public Expr as(Expr expr, String newType) {
		return new UseAsType(this, expr, newType);
	}

	@Override
	public AssignExpr assign(Var assignTo, Expr expr) {
		return new AssignExpr(this, assignTo, expr);
	}

	@Override
	public Expr assign(FieldExpr field, Expr expr) {
		return new AssignExpr(this, field, expr);
	}

	@Override
	public BoolConstExpr boolConst(boolean b) {
		return new BoolConstExpr(this, b);
	}

	@Override
	public Expr box(Expr expr) {
		return new BoxExpr(this, expr);
	}

	@Override
	public Expr unbox(Expr expr) {
		return new UnboxExpr(this, expr);
	}

	/** Group a collection of expressions as a block */
	@Override
	public Expr block(Expr... exprs) {
		return new BlockExpr(this, exprs);
	}

	@Override
	public Expr callSuper(String returns, String parentClzName, String methodName, Expr... args) {
		return new MethodInvocation(this, "super", returns, myThis(), parentClzName, methodName, args);
	}
	
	@Override
	public Expr callVirtual(String returns, Expr obj, String methodName, Expr... args) {
		return new MethodInvocation(this, "virtual", returns, obj, null, methodName, args);
	}
	
	@Override
	public Expr callInterface(String returns, Expr obj, String methodName, Expr... args) {
		return new MethodInvocation(this, "interface", returns, obj, null, methodName, args);
	}
	
	@Override
	public Expr callStatic(String inClz, String returns, String methodName, Expr...args) {
		return new MethodInvocation(this, "static", returns, null, inClz, methodName, args);
	}

	@Override
	public Expr castTo(Expr expr, String ofType) {
		return new CastToExpr(this, expr, ofType);
	}
	
	@Override
	public ClassConstExpr classConst(String cls) {
		return new ClassConstExpr(this, cls);
	}

	@Override
	public Expr concat(Object... args) {
		return new ConcatExpr(this, args);
	}

	@Override
	public IfExpr ifBoolean(Expr expr, Expr then, Expr orelse) {
		return new IfExpr(this, expr, then, orelse);
	}

	@Override
	public Expr ifEquals(Expr left, Expr right, Expr then, Expr orelse)
	{
		return new IfExpr(this, new EqualsExpr(this, left, right), then, orelse);
	}


	@Override
	public Expr ifNotNull(Expr test, Expr then,Expr orelse) {
		return new IfExpr(this, test, then, orelse, IfCond.NOTNULL);
	}

	@Override
	public Expr ifNull(Expr test, Expr then, Expr orelse)
	{
		return new IfExpr(this, test, then, orelse, IfCond.NULL);
	}

	@Override
	public Expr isNull(Expr test, Expr yes, Expr no) {
		return new IsExpr(this, test, yes, no, IfCond.NULL);
	}

	@Override
	public IntConstExpr intConst(int i) {
		return new IntConstExpr(this, i);
	}


	@Override
	public MakeNewExpr makeNew(String ofClz, Expr... args) {
		return new MakeNewExpr(this, ofClz, args);
	}

	@Override
	public Expr returnVoid() {
		return new ReturnX(this, "void", null);
	}

	@Override
	public Expr returnBool(Expr i) {
		return new ReturnX(this, "boolean", i);
	}

	@Override
	public Expr returnInt(Expr i) {
		return new ReturnX(this, "int", i);
	}

	@Override
	public Expr returnObject(Expr e) {
		return new ReturnX(this, "object", e);
	}

	@Override
	public StringConstExpr stringConst(String str) {
		return new StringConstExpr(this, str);
	}

	@Override
	public Expr throwException(String clz, Expr... args)
	{
		return new ThrowExpr(this, clz, args);
	}

	@Override
	public Expr trueConst() {
		return as(intConst(1), "boolean");
	}
	
	@Override
	public Expr voidExpr(Expr ignoredResult) {
		return new VoidExpr(this, ignoredResult);
	}

	@Override
	public void throwsException(String exception) {
		exceptions.add(exception);
	}

	@Override
	public void complete() throws IOException {
		for (AttributeInfo a : attributes)
			if (a.hasName("Code"))
				throw new UtilException("Cannot complete a method that already has a Code attribute");

		if (access_flags == -1)
			access_flags = ByteCodeFile.ACC_PUBLIC;
		if (isStatic)
			access_flags |= ByteCodeFile.ACC_STATIC;
		
		for (AnnotationType at : annotations)
		{
			at.addTo(bcf, attributes, annotations.get(at), arguments.size());
		}

		if (exceptions.size() != 0)
		{
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeShort(exceptions.size());
				for (String s : exceptions)
					dos.writeShort(bcf.pool.requireClass(s));
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
		descriptorIdx = bcf.pool.requireUtf8(signature());
	}

	private String signature() {
		List<String> mapto = new ArrayList<String>();
		for (Var v : arguments)
			mapto.add(JavaInfo.map(v.getType()));
		return signature(returnType, mapto);
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
		if (lenientMode)
			System.err.println(instruction + " stack = " + opdepth + " change = " + stackChange);
		opstack(stackChange);
	}

	private void opstack(int i) {
		opdepth += i;
		if (opdepth < 0 && !lenientMode)
			throw new UtilException("Stack underflow generating " + name + " in " + bcf);
		if (opdepth > maxStack)
			maxStack = opdepth;
	}
	
	@Override
	public void aconst_null() {
		add(1, new Instruction(0x1));
	}
	
	@Override
	public void aload(int i) {
		if (i < 4)
			add(1, new Instruction(0x2a+i));
		else
			add(1, new Instruction(0x19, i));
	}
	
	@Override
	public void anewarray(String clz)
	{
		int idx = bcf.pool.requireClass(clz);
		add(0, new Instruction(0xbd, hi(idx), lo(idx)));
	}

	@Override
	public void areturn() {
		add(-1, new Instruction(0xb0));
	}

	@Override
	public void astore(int i) {
		if (i < 4)
			add(-1, new Instruction(0x4b+i));
		else
			add(-1, new Instruction(0x3a, i));
		if (i >= locals)
			locals = i+1;
	}

	@Override
	public void athrow() {
		add(-1, new Instruction(0xbf));
	}
	
	@Override
	public void checkCast(String clz)
	{
		int idx = bcf.pool.requireClass(JavaInfo.mapPrimitive(clz));
		add(0, new Instruction(0xc0, idx>>8, idx &0xff));
	}

	@Override
	public void dload(int i) {
		// We add 2 to the stack because double is 8 bytes or whatever
		if (i < 4)
			add(2, new Instruction(0x26+i));
		else
			add(2, new Instruction(0x18, i));
	}

	@Override
	public void dreturn() {
		// We subtract 2 from the stack because double is 8 bytes or whatever
		add(-2, new Instruction(0xaf));
	}

	@Override
	public void dstore(int i) {
		if (i < 4)
			add(-2, new Instruction(0x47+i));
		else
			add(-2, new Instruction(0x39, i));
		if (i >= locals)
			locals = i+1;
	}

	@Override
	public void dup() {
		add(1, new Instruction(0x59));
	}

	@Override
	public void getField(String clz, String type, String var) {
		int clzIdx = bcf.pool.requireClass(clz);
		int fieldIdx = bcf.pool.requireUtf8(var);
		int sigIdx = bcf.pool.requireUtf8(map(type));
		int ntIdx = bcf.pool.requireNT(fieldIdx, sigIdx);
		int idx = bcf.pool.requireRef(ByteCodeFile.CONSTANT_Fieldref, clzIdx, ntIdx);
		add(0, new Instruction(0xb4, hi(idx), lo(idx)));
	}

	@Override
	public void getStatic(String clz, String type, String var) {
		int clzIdx = bcf.pool.requireClass(clz);
		int fieldIdx = bcf.pool.requireUtf8(var);
		int sigIdx = bcf.pool.requireUtf8(map(type));
		int ntIdx = bcf.pool.requireNT(fieldIdx, sigIdx);
		int idx = bcf.pool.requireRef(ByteCodeFile.CONSTANT_Fieldref, clzIdx, ntIdx);
		add(1, new Instruction(0xb2, hi(idx), lo(idx)));
	}

	@Override
	public void iconst(int i) {
		if (i >= -1 && i <=5)
		{
			i += 0x3;
			add(1, new Instruction(i));
		}
		else
		{
			// TODO: should be able, just not yet supported.
			throw new UtilException("Cannot make iconst(" + i + ")");
		}
	}

	@Override
	public Marker ifeq() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0x99, 00, 00));
		return ret;
	}
	
	@Override
	public Marker ifne() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0x9a, 00, 00));
		return ret;
	}

	@Override
	public Marker iflt() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0x9b, 00, 00));
		return ret;
	}

	@Override
	public Marker ifge() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0x9c, 00, 00));
		return ret;
	}

	@Override
	public Marker ifgt() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0x9d, 00, 00));
		return ret;
	}

	@Override
	public Marker ifle() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0x9e, 00, 00));
		return ret;
	}

	@Override
	public Marker ifnull() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0xc6, 00, 00));
		return ret;
	}

	@Override
	public Marker ifnonnull() {
		Marker ret = new Marker(instructions, 1);
		add(-1, new Instruction(0xc7, 00, 00));
		return ret;
	}

	@Override
	public void iload(int i) {
		if (i < 4)
			add(1, new Instruction(0x1a+i));
		else
			add(1, new Instruction(0x15, i));
	}

	@Override
	public void istore(int i) {
		if (i < 4)
			add(-1, new Instruction(0x3b+i));
		else
			add(-1, new Instruction(0x36, i));
		if (i >= locals)
			locals = i+1;
	}

	@Override
	public void invokeOtherConstructor(String clz,	String... args) {
		invoke(0xb7, false, clz, "void", "<init>", args);
	}
	
	@Override
	public void invokeParentConstructor(String... args) {
		if (byteCodeCreator.getSuperClass() == null)
			throw new UtilException("Cannot use parent methods without defining superclass");
		invoke(0xb7, false, byteCodeCreator.getSuperClass(), "void", "<init>", args);
	}

	@Override
	public void invokeParentMethod(String typeReturn, String method, String... args) {
		if (byteCodeCreator.getSuperClass() == null)
			throw new UtilException("Cannot use parent methods without defining superclass");
		invoke(0xb7, false, byteCodeCreator.getSuperClass(), typeReturn, method, args);
	}

	@Override
	public void invokeStatic(String clz, String typeReturn, String method, String... args) {
		invoke(0xb8, true, clz, typeReturn, method, args);
	}

	private int invokeIdx(String clz, String ret, String meth, byte refType, String... args) {
		int clzIdx = bcf.pool.requireClass(clz);
		int methIdx = bcf.pool.requireUtf8(meth);
		int sigIdx = bcf.pool.requireUtf8(signature(map(ret), Lambda.map(mapType, CollectionUtils.listOf(args))));
		int ntIdx = bcf.pool.requireNT(methIdx, sigIdx);
		int idx = bcf.pool.requireRef(refType, clzIdx, ntIdx);
		return idx;
	}
	
	private void addInvoke(Instruction instruction, boolean isStatic, String ret, String... args) {
		int pop = args.length;
		if (ret.equals("void"))
			++pop;
		else if (ret.equals("double") || ret.equals("long"))
			--pop;
		for (String s : args)
			if (s.equals("double") || s.equals("long"))
				++pop;
		if (isStatic)
			--pop;
		add(-pop, instruction);
	}
	
	private void invoke(int opcode, boolean isStatic, String clz, String ret, String meth, String... args) {
		int idx = invokeIdx(clz, ret, meth, ByteCodeFile.CONSTANT_Methodref, args);
		addInvoke(new Instruction(opcode, hi(idx), lo(idx)), isStatic, ret, args);
	}

	@Override
	public void invokeVirtualMethod(String clz, String ret, String method, String... args) {
		invoke(0xb6, false, clz, ret, method, args);
	}

	@Override
	public void invokeInterface(String clz, String ret, String method, String... args) {
		int idx = invokeIdx(clz, ret, method, ByteCodeFile.CONSTANT_Interfaceref, args);
		int count = args.length+1;
		for (String s : args)
			if (s.equals("double") || s.equals("long"))
				count++;
		addInvoke(new Instruction(0xb9, hi(idx), lo(idx), count, 0), false, ret, args);
	}

	@Override
	public void ireturn() {
		add(-1, new Instruction(0xac));
	}

	@Override
	public Marker jump() {
		Marker ret = new Marker(instructions, 1);
		add(0, new Instruction(0xa7, 00, 00));
		return ret;
	}

	@Override
	public void ldcClass(String clz)
	{
		int idx = bcf.pool.requireClass(JavaInfo.mapPrimitive(clz));
		if (idx < 256)
			add(1, new Instruction(0x12, idx));
		else
			add(1, new Instruction(0x13, hi(idx), lo(idx)));
	}

	@Override
	public void ldcString(String string) {
		int idx = bcf.pool.requireString(string);
		if (idx < 256)
			add(1, new Instruction(0x12, idx));
		else
			add(1, new Instruction(0x13, hi(idx), lo(idx)));
	}

	@Override
	public void lload(int i) {
		if (i < 4)
			add(2, new Instruction(0x1e+i));
		else
			add(2, new Instruction(0x16, i));
	}

	@Override
	public void lreturn() {
		add(-2, new Instruction(0xad));
	}
	
	@Override
	public void lstore(int i) {
		if (i < 4)
			add(-2, new Instruction(0x3f+i));
		else
			add(-2, new Instruction(0x37, i));
		if (i >= locals)
			locals = i+1;
	}

	@Override
	public void newObject(String clz) {
		int idx = bcf.pool.requireClass(clz);
		add(1, new Instruction(0xbb, idx>>8,idx&0xff));
	}

	@Override
	public void pop(String type) {
		if (type.equals("void"))
			return;
		if (type.equals("double") || type.equals("long"))
			add(-2, new Instruction(0x58));
		else
			add(-1, new Instruction(0x57));
	}

	@Override
	public void putField(String clz, String type, String var) {
		int clzIdx = bcf.pool.requireClass(clz);
		int fieldIdx = bcf.pool.requireUtf8(var);
		int sigIdx = bcf.pool.requireUtf8(map(type));
		int ntIdx = bcf.pool.requireNT(fieldIdx, sigIdx);
		int idx = bcf.pool.requireRef(ByteCodeFile.CONSTANT_Fieldref, clzIdx, ntIdx);
		int pop = -2;
		if (type.equals("double") || type.equals("long"))
			pop = -3;
		add(pop, new Instruction(0xb5, idx>>8, idx&0xff));
	}

	@Override
	public void putStatic(String clz, String type, String var) {
		int clzIdx = bcf.pool.requireClass(clz);
		int fieldIdx = bcf.pool.requireUtf8(var);
		int sigIdx = bcf.pool.requireUtf8(map(type));
		int ntIdx = bcf.pool.requireNT(fieldIdx, sigIdx);
		int idx = bcf.pool.requireRef(ByteCodeFile.CONSTANT_Fieldref, clzIdx, ntIdx);
		int pop = -1;
		if (type.equals("double") || type.equals("long"))
			pop = -2;
		add(pop, new Instruction(0xb3, idx>>8, idx&0xff));
	}

	@Override
	public void vreturn() {
		add(0, new Instruction(0xb1));
	}

	@Override
	public Annotation addRTVAnnotation(String attrClass) {
		Annotation ret = new Annotation(bcf, attrClass);
		annotations.add(AnnotationType.RuntimeVisibleAnnotations, ret);
		return ret;
	}

	@Override
	public Annotation addRTVPAnnotation(String attrClass, int param) {
		Annotation ret = new Annotation(bcf, attrClass, param);
		annotations.add(AnnotationType.RuntimeVisibleParameterAnnotations, ret);
		return ret;
	}

	@Override
	public int nextLocal() {
		return locals++;
	}

	@Override
	public String getClassName() {
		return byteCodeCreator.getCreatedName();
	}
	
	public int stackDepth()
	{
		return opdepth;
	}
	
	public void resetStack(int to)
	{
		opdepth = to;
	}
	
	@Override
	public String toString() {
		return "Method[" + returnType + " " + getClassName() + "::" + this.name + "]";
	}
}
