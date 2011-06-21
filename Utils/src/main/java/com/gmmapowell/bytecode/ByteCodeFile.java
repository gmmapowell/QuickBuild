package com.gmmapowell.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gmmapowell.bytecode.CPInfo.ClassInfo;
import com.gmmapowell.bytecode.CPInfo.DoubleEntry;
import com.gmmapowell.bytecode.CPInfo.DoubleInfo;
import com.gmmapowell.bytecode.CPInfo.FloatInfo;
import com.gmmapowell.bytecode.CPInfo.IntegerInfo;
import com.gmmapowell.bytecode.CPInfo.LongInfo;
import com.gmmapowell.bytecode.CPInfo.NTInfo;
import com.gmmapowell.bytecode.CPInfo.RefInfo;
import com.gmmapowell.bytecode.CPInfo.StringInfo;
import com.gmmapowell.bytecode.CPInfo.Utf8Info;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class ByteCodeFile {
	public final static int   javaMagic        = 0xCAFEBABE;
	public final static short javaMajorVersion = 50; // Java 1.6?
	public final static short javaMinorVersion = 0;
	public final static short ACC_PUBLIC       = 0x0001;
	public final static short ACC_PRIVATE      = 0x0002;
	public final static short ACC_PROTECTED    = 0x0004;
	public final static short ACC_STATIC       = 0x0008;
	public final static short ACC_FINAL        = 0x0010;
	public final static short ACC_SUPER        = 0x0020;
	public final static short ACC_SYNCHRONIZED = 0x0020;
	public final static short ACC_NATIVE       = 0x0100;
	public final static short ACC_INTERFACE    = 0x0200;
	public final static short ACC_ABSTRACT     = 0x0400;
	public final static short ACC_STRICT       = 0x0800;

	public final static byte CONSTANT_UTF8         = 1;
	public final static byte CONSTANT_Integer      = 3;
	public final static byte CONSTANT_Float        = 4;
	public final static byte CONSTANT_Long         = 5;
	public final static byte CONSTANT_Double       = 6;
	public final static byte CONSTANT_Class        = 7;
	public final static byte CONSTANT_String       = 8;
	public final static byte CONSTANT_Fieldref     = 9;
	public final static byte CONSTANT_Methodref    = 10;
	public final static byte CONSTANT_Interfaceref = 11;
	public final static byte CONSTANT_NameAndType  = 12;
	
	protected CPInfo[] pool;
	protected List<Integer> interfaces = new ArrayList<Integer>();
	private int nextPoolEntry = 1;
	private int access_flags = -1;
	private int this_idx = -1;
	private int super_idx = -1;
	private List<FieldInfo> fields = new ArrayList<FieldInfo>();
	private List<MethodInfo> methods = new ArrayList<MethodInfo>();
	private List<AttributeInfo> attributes = new ArrayList<AttributeInfo>();
	private final String qualifiedName;
	
	public ByteCodeFile(File from, String qualifiedName)
	{
		this.qualifiedName = qualifiedName;
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(from);
			read(fis);
			fis.close();
		}
		catch (IOException ex)
		{
			if (fis != null)
				try { fis.close(); } catch (IOException e2) { }
			throw UtilException.wrap(ex);
		}
	}
	
	public ByteCodeFile(InputStream fis)
	{
		read(fis);
		qualifiedName = null;
	}

	private void read(InputStream fis) {
		try
		{
			DataInputStream dis = new DataInputStream(fis);
			int magic = dis.readInt();
			if (magic != javaMagic)
				throw new UtilException("This is not a bytecode file");
			dis.readUnsignedShort(); //minor
			dis.readUnsignedShort(); // major
			readConstantPool(dis);
			access_flags = dis.readUnsignedShort(); // access_flags
			this_idx = dis.readUnsignedShort(); // this_class
			super_idx = dis.readUnsignedShort(); // super_class
			readInterfaces(dis);
			readFields(dis);
			readMethods(dis);
			readAttributes(dis, attributes);
			if (dis.available() != 0)
				throw new UtilException("There are still " + dis.available() + " bytes available on the stream");
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	protected ByteCodeFile(String qualifiedName)
	{
		this.qualifiedName = qualifiedName;
	}

	protected ByteCodeFile()
	{
		qualifiedName = null;
	}
	
	public void makeInterface()
	{
		access_flags = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;
	}

	public void write(DataOutputStream dos) throws IOException {
		if (access_flags == -1)
			access_flags = ACC_SUPER | ACC_PUBLIC;
		if (this_idx == -1)
			throw new UtilException("You must specify a this class");
		if (super_idx == -1)
			super_idx = requireClass("java/lang/Object");

		for (MethodInfo mi : methods)
			((MethodCreator)mi).complete();
		
		dos.writeInt(javaMagic);
		dos.writeShort(0);
		dos.writeShort(50);
		writeConstantPool(dos);
		dos.writeShort(access_flags);
		dos.writeShort(this_idx);
		dos.writeShort(super_idx);
		writeInterfaces(dos);
		writeFields(dos);
		writeMethods(dos);
		writeAttributes(dos, attributes);
	}

	private void readConstantPool(DataInputStream dis) throws IOException {
		int poolCount = dis.readUnsignedShort();
		pool = new CPInfo[poolCount];

		// This is weird offsetting ...
		for (int idx=1;idx<poolCount;idx++)
		{
			pool[idx] = readPoolEntry(dis);
			if (pool[idx] instanceof DoubleEntry)
				idx++; // skip the second entry
//				System.out.println(idx + " = " + pool[idx]);
		}
	}

	public short addPoolEntry(CPInfo entry)
	{
		if (pool == null)
			pool = new CPInfo[10];
		else if (nextPoolEntry >= pool.length)
		{
			pool = Arrays.copyOf(pool, pool.length*2);
		}
		short ret = (short) nextPoolEntry;
		pool[nextPoolEntry++] = entry;
		if (entry instanceof DoubleEntry)
			nextPoolEntry++;
		return ret;
	}

	private void writeConstantPool(DataOutputStream dos) throws IOException {
		dos.writeShort(nextPoolEntry);
		for (int idx=1;idx<nextPoolEntry;idx++)
		{
			if (pool[idx] == null)
				continue;
			pool[idx].writeEntry(dos);
			if (pool[idx] instanceof DoubleEntry)
				idx++;
		}
	}

	private void readInterfaces(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		for (int i=0;i<cnt;i++)
			interfaces.add(dis.readUnsignedShort()); // the pool id of the interface
	}

	private void writeInterfaces(DataOutputStream dos) throws IOException {
		dos.writeShort(interfaces.size());
		for (int ci : interfaces)
			dos.writeShort(ci);
	}

	private void readFields(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		// System.out.println("# of fields = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			FieldInfo fi = new FieldInfo(this);
			dis.readUnsignedShort(); // access_flags
			@SuppressWarnings("unused")
			int name = dis.readUnsignedShort(); // name idx
//			System.out.println("Reading field " + pool[name]);
			dis.readUnsignedShort(); // descriptor idx
			readAttributes(dis, fi.attributes);
		}
		
	}

	private void writeFields(DataOutputStream dos) throws IOException {
		dos.writeShort(fields.size());
		for (FieldInfo fi : fields)
		{
			fi.write(dos);
		}
	}

	private void readMethods(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		// System.out.println("# of methods = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			MethodInfo mi = new MethodInfo(this);
			mi.access_flags = (short) dis.readUnsignedShort();
			mi.nameIdx = (short) dis.readUnsignedShort();
			mi.descriptorIdx = (short) dis.readUnsignedShort();
			readAttributes(dis, mi.attributes);
			methods.add(mi);
		}
		
	}

	private void writeMethods(DataOutputStream dos) throws IOException {
		dos.writeShort(methods.size());
		for (MethodInfo mi : methods)
		{
			mi.write(dos);
		}
	}

	private void readAttributes(DataInputStream dis, List<AttributeInfo> attrs) throws IOException {
		int cnt = dis.readUnsignedShort();
		for (int i=0;i<cnt;i++)
		{
			int idx = dis.readUnsignedShort();
			int len = dis.readInt();
			byte[] bytes = new byte[len];
			readBytes(dis, bytes);
			attrs.add(new AttributeInfo(pool, idx, bytes));
		}
		return;
	}

	void writeAttributes(DataOutputStream dos, List<AttributeInfo> attrs) throws IOException {
		dos.writeShort(attrs.size());
		for (AttributeInfo ai : attrs)
		{
			dos.writeShort(ai.nameIdx);
			dos.writeInt(ai.getBytes().length);
			dos.write(ai.getBytes());
		}
	}

	private CPInfo readPoolEntry(DataInputStream dis) throws IOException {
		int tag = dis.readUnsignedByte();
		switch (tag)
		{
		case CONSTANT_UTF8:
			return readUtf8Entry(dis);
		case CONSTANT_Integer:
			return readIntegerEntry(dis);
		case CONSTANT_Float:
			return readFloatEntry(dis);
		case CONSTANT_Long:
			return readLongEntry(dis);
		case CONSTANT_Double:
			return readDoubleEntry(dis);
		case CONSTANT_Class:
			return readClassEntry(dis);
		case CONSTANT_String:
			return readStringEntry(dis);
		case CONSTANT_Fieldref:
		case CONSTANT_Methodref:
		case CONSTANT_Interfaceref:
			return readRefEntry(dis, tag);
		case CONSTANT_NameAndType:
			return readNameAndType(dis);
		default:
		{
			/*
			for (int i=0;i<pool.length;i++)
				System.out.println(i + ": " + pool[i]);
				*/
			throw new UtilException("There is no handler for tag " + tag);
		}
		}
	}

	private Utf8Info readUtf8Entry(DataInputStream dis) throws IOException {
		int len = dis.readUnsignedShort();
		byte[] bytes = new byte[len];
		readBytes(dis, bytes);
		return new Utf8Info(bytes);
	}
	
	private IntegerInfo readIntegerEntry(DataInputStream dis) throws IOException {
		return new IntegerInfo(dis.readInt());
	}
	
	private FloatInfo readFloatEntry(DataInputStream dis) throws IOException {
		return new FloatInfo(dis.readFloat());
	}

	private LongInfo readLongEntry(DataInputStream dis) throws IOException {
		int high = dis.readInt();
		int low = dis.readInt();
		return new LongInfo(high, low);
	}

	private DoubleInfo readDoubleEntry(DataInputStream dis) throws IOException {
		double val = dis.readDouble();
		return new DoubleInfo(val);
	}
	
	private ClassInfo readClassEntry(DataInputStream dis) throws IOException {
		int idx = dis.readUnsignedShort();
		return new ClassInfo(pool, idx);
	}

	private StringInfo readStringEntry(DataInputStream dis) throws IOException {
		int idx = dis.readUnsignedShort();
		return new StringInfo(pool, idx);
	}

	private RefInfo readRefEntry(DataInputStream dis, int tag) throws IOException {
		int clz = dis.readUnsignedShort();
		int nt = dis.readUnsignedShort();
		return new RefInfo(pool, clz, nt, tag);
	}

	private NTInfo readNameAndType(DataInputStream dis) throws IOException {
		int name = dis.readUnsignedShort();
		int descriptor = dis.readUnsignedShort();
		return new NTInfo(pool, name, descriptor);
	}

	public void addInterface(String intf) {
		int idx = requireClass(intf);
		interfaces.add(idx);
	}

	public boolean implementsInterface(Class<?> class1) {
		String name = FileUtils.convertDottedToSlashPath(class1.getCanonicalName());
		for (int idx : interfaces)
		{
			ClassInfo c = (ClassInfo)pool[idx];
			if (c.equals(name))
				return true;
		}
		return false;
	}

	protected void readBytes(DataInputStream dis, byte[] bytes) throws IOException {
		int off = 0;
		while (off < bytes.length)
		{
			int cnt = dis.read(bytes, off, bytes.length-off);
			off += cnt;
		}
	}

	public void thisClass(String name) {
		if (this_idx != -1)
			throw new UtilException("Cannot define 'this' class twice");
		this_idx = requireClass(name);
	}

	public void superClass(String name) {
		if (super_idx != -1)
			throw new UtilException("Cannot define 'super' class twice");
		super_idx = requireClass(name);
	}

	public void addField(FieldInfo field) {
		fields.add(field);
	}

	public void addMethod(MethodCreator ret) {
		methods.add(ret);
	}

	int requireClass(String string) {
		String s = FileUtils.convertDottedToSlashPath(string);
		int utf8Idx = 0;
		for (int i=1;i<nextPoolEntry;i++)
			if (pool[i] != null && pool[i] instanceof CPInfo.Utf8Info && ((CPInfo.Utf8Info)pool[i]).asString().equals(s))
			{
				utf8Idx = i;
				break;
			}
		if (utf8Idx > 0)
		{
			for (int i=1;i<nextPoolEntry;i++)
				if (pool[i] != null && pool[i] instanceof CPInfo.ClassInfo && ((CPInfo.ClassInfo)pool[i]).idx == utf8Idx)
					return i;
		}
		else
			utf8Idx = nextPoolEntry+1;
		int clzIdx = nextPoolEntry;
		addPoolEntry(new CPInfo.ClassInfo(pool, utf8Idx));
		addPoolEntry(new CPInfo.Utf8Info(s));
		return clzIdx;
	}

	public short requireUtf8(String name) {
		for (short i=1;i<nextPoolEntry;i++)
			if (pool[i] != null && pool[i] instanceof CPInfo.Utf8Info && ((CPInfo.Utf8Info)pool[i]).asString().equals(name))
			{
				return i;
			}
		return addPoolEntry(new CPInfo.Utf8Info(name));
	}
	
	public int requireNT(int methIdx, int sigIdx) {
		for (short i=1;i<nextPoolEntry;i++)
			if (pool[i] != null && pool[i] instanceof CPInfo.NTInfo)
			{
				CPInfo.NTInfo nt = (CPInfo.NTInfo)pool[i];
				if (nt.isA(methIdx, sigIdx))
					return i;
			}
		return addPoolEntry(new CPInfo.NTInfo(pool, methIdx, sigIdx));
	}

	public int requireRef(int refType, int clzIdx, int ntIdx) {
		for (short i=1;i<nextPoolEntry;i++)
			if (pool[i] != null && pool[i] instanceof CPInfo.RefInfo)
			{
				CPInfo.RefInfo r = (CPInfo.RefInfo)pool[i];
				if (r.isA(refType, clzIdx, ntIdx))
					return i;
			}
		return addPoolEntry(new CPInfo.RefInfo(pool, clzIdx, ntIdx, refType));
	}

	public int requireString(String s) {
		int utf8Idx = 0;
		for (int i=1;i<nextPoolEntry;i++)
			if (pool[i] != null && pool[i] instanceof CPInfo.Utf8Info && ((CPInfo.Utf8Info)pool[i]).asString().equals(s))
			{
				utf8Idx = i;
				break;
			}
		if (utf8Idx > 0)
		{
			for (int i=1;i<nextPoolEntry;i++)
				if (pool[i] != null && pool[i] instanceof CPInfo.StringInfo && ((CPInfo.StringInfo)pool[i]).idx == utf8Idx)
					return i;
		}
		else
			utf8Idx = nextPoolEntry+1;
		int strIdx = nextPoolEntry;
		addPoolEntry(new CPInfo.StringInfo(pool, utf8Idx));
		addPoolEntry(new CPInfo.Utf8Info(s));
		return strIdx;
	}
	
	@Override
	public String toString() {
		if (qualifiedName != null)
			return "BCF[" + qualifiedName + "]";
		return super.toString();
	}

	public boolean hasMethodsWithAnnotation(String string) {
		String mapped = JavaInfo.map(string);
		for (MethodInfo mi : methods)
		{
			for (AttributeInfo ai : mi.attributes)
			{
				if (ai.hasName("RuntimeVisibleAnnotations"))
				{
					RuntimeVisibleAnnotations rva = new RuntimeVisibleAnnotations(this, ai);
					if (rva.has(mapped))
						return true;
				}
			}
		}
		return false;
	}

	public AttributeInfo newAttribute(String named, byte[] data) {
		return new AttributeInfo(pool, requireUtf8(named), data);
	}
}
