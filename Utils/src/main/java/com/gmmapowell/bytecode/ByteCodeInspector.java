package com.gmmapowell.bytecode;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

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
import com.gmmapowell.utils.StringUtil;

public class ByteCodeInspector extends ByteCodeFile {

	private HexDumpStream hexdump;

	public static class HexDumpStream extends InputStream {

		private final InputStream fis;
		private int pos = 0;
		private int cnt = 0;
		private int wrap = 16;
		private byte[] str;
		private final PrintWriter out;

		public HexDumpStream(PrintWriter out, InputStream fis) {
			this.fis = fis;
			str = new byte[wrap];
			if (out == null)
				this.out = new PrintWriter(System.out);
			else
				this.out = out;
		}

		@Override
		public int read() throws IOException {
			int b = fis.read();
			if (cnt >= wrap)
			{
				out.println(new String(str));
				cnt = 0;
			}
			if (cnt == 0)
			{
				out.print(StringUtil.hex(pos, 8)+ " ");
			}
			if (b > 32 && b < 127)
				str[cnt] = (byte) b;
			else
				str[cnt] = '.';
			out.print(StringUtil.hex(b, 2) + " ");
			cnt++;
			pos++;
			return b;
		}

		public void print(Object obj) {
			for (int i=cnt;i<wrap;i++)
			{
				out.print("   ");
				str[i] = ' ';
			}
			out.print(new String(str) + "  ");
			out.println(obj);
			cnt = 0;
		}

	}

	public static void main(String[] args)
	{
		try
		{
			ByteCodeInspector bci = new ByteCodeInspector();
			InputStream fis = ByteCodeInspector.class.getResourceAsStream(args[0]);
			if (fis == null)
				throw new RuntimeException("Could not find " + fis);
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("C:\\tmp\\dump.txt"));
			bci.read(new PrintWriter(writer), fis);
			writer.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.out);
		}
	}
		
	public void read(PrintWriter out, InputStream fis)
	{
		try
		{
			hexdump = new HexDumpStream(out, fis);
			DataInputStream dis = new DataInputStream(hexdump);
			int magic = dis.readInt();
			if (magic != javaMagic)
				throw new UtilException("This is not a bytecode file");
			hexdump.print("Magic: " + magic);
			int minorVersion = dis.readUnsignedShort(); //minor
			int majorVersion = dis.readUnsignedShort(); // major
			hexdump.print("Version: " + majorVersion + "-" + minorVersion);
			readConstantPool(dis);
			int access = dis.readUnsignedShort(); // access_flags
			hexdump.print("Access = "  + access);
			int thisClass = dis.readUnsignedShort(); // this_class
			hexdump.print("This = " + pool[thisClass]);
			int superClass = dis.readUnsignedShort(); // super_class
			hexdump.print("Super = " + pool[superClass]);
			readInterfaces(dis);
			readFields(dis);
			readMethods(dis);
			hexdump.print("Class Attributes");
			readAttributes(dis);
			/*
			if (dis.available() != 0)
				throw new UtilException("There are still " + dis.available() + " bytes available on the stream");
				*/
		}
		catch (Exception ex)
		{
			try {
				out.close();
			} catch (Exception e2) { }
			throw UtilException.wrap(ex);
		}
	}

	private void readConstantPool(DataInputStream dis) throws IOException {
		int poolCount = dis.readUnsignedShort();
		pool = new CPInfo[poolCount];
		hexdump.print("pool has " + poolCount + " entries, including #0");

		// This is weird offsetting ...
		for (int idx=1;idx<poolCount;idx++)
		{
			pool[idx] = readPoolEntry(dis);
			hexdump.print(idx +" => " + pool[idx]);
			if (pool[idx] instanceof DoubleEntry)
			{
				hexdump.print("****");
				idx++; // skip the second entry
			}
//				System.out.println(idx + " = " + pool[idx]);
		}
	}
	private void readInterfaces(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		hexdump.print(cnt + " interfaces");
		for (int i=0;i<cnt;i++)
		{
			ClassInfo intf = (ClassInfo) pool[dis.readUnsignedShort()];
			interfaces.add(intf);
			hexdump.print(intf);
		}
	}

	private void readFields(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		hexdump.print("# of fields = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			int access = dis.readUnsignedShort(); // access_flags
			int name = dis.readUnsignedShort(); // name idx
//			System.out.println("Reading field " + pool[name]);
			int descriptor = dis.readUnsignedShort(); // descriptor idx
			hexdump.print("Field " + pool[name] + " " + pool[descriptor] + " " + access);
			readAttributes(dis);
		}
		
	}

	private void readMethods(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		hexdump.print("# of methods = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			int access = dis.readUnsignedShort(); // access_flags
			int name = dis.readUnsignedShort(); // name idx
			int descriptor = dis.readUnsignedShort(); // descriptor idx
			hexdump.print("Method " + pool[name] + " " + pool[descriptor] + " " + access);
			readAttributes(dis);
		}
		
	}
	private void readAttributes(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		if (cnt > 0)
			hexdump.print(cnt + " attributes");
		for (int i=0;i<cnt;i++)
		{
			int idx = dis.readUnsignedShort(); // name_index
			int len = dis.readInt(); // length
			if (len > 5000)
				throw new RuntimeException("What?");
			hexdump.print("idx = " + idx + ": " + pool[idx] + " len=" + len);
			byte[] bytes = new byte[len];
			readBytes(dis, bytes);
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
}