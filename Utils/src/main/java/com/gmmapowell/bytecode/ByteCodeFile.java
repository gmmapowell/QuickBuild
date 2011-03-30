package com.gmmapowell.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
	
	private CPInfo[] pool;
	private List<ClassInfo> interfaces = new ArrayList<ClassInfo>();
	
	public ByteCodeFile(InputStream fis)
	{
		try
		{
			DataInputStream dis = new DataInputStream(fis);
			int magic = dis.readInt();
			if (magic != javaMagic)
				throw new UtilException("This is not a bytecode file");
			dis.readUnsignedShort(); //minor
			dis.readUnsignedShort(); // major
			readConstantPool(dis);
			dis.readUnsignedShort(); // access_flags
			dis.readUnsignedShort(); // this_class
			dis.readUnsignedShort(); // super_class
			readInterfaces(dis);
			readFields(dis);
			readMethods(dis);
			readAttributes(dis);
			if (dis.available() != 0)
				throw new UtilException("There are still " + dis.available() + " bytes available on the stream");
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
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

	private void readInterfaces(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		for (int i=0;i<cnt;i++)
			interfaces.add((ClassInfo) pool[dis.readUnsignedShort()]); // the pool id of the interface
	}

	private void readFields(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		// System.out.println("# of fields = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			dis.readUnsignedShort(); // access_flags
			dis.readUnsignedShort(); // name idx
			dis.readUnsignedShort(); // descriptor idx
			readAttributes(dis);
		}
		
	}

	private void readMethods(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		// System.out.println("# of methods = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			dis.readUnsignedShort(); // access_flags
			dis.readUnsignedShort(); // name idx
			dis.readUnsignedShort(); // descriptor idx
			readAttributes(dis);
		}
		
	}

	private void readAttributes(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		for (int i=0;i<cnt;i++)
		{
			int idx = dis.readUnsignedShort(); // name_index
			int len = dis.readInt(); // length
			byte[] bytes = new byte[len];
			dis.read(bytes);
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
			for (int i=0;i<pool.length;i++)
				System.out.println(i + ": " + pool[i]);
			throw new UtilException("There is no handler for tag " + tag);
		}
		}
	}

	private Utf8Info readUtf8Entry(DataInputStream dis) throws IOException {
		int len = dis.readUnsignedShort();
		byte[] bytes = new byte[len];
		dis.read(bytes);
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

	public boolean implementsInterface(Class<?> class1) {
		String name = FileUtils.convertDottedToSlashPath(class1.getCanonicalName());
		for (ClassInfo c : interfaces)
			if (c.equals(name))
				return true;
		return false;
	}
}
