package com.gmmapowell.bytecode;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
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
import com.gmmapowell.utils.StringUtil;

public class ByteCodeInspector extends ByteCodeFile {

	private HexDumpStream hexdump;
	private boolean cleanMode;
	private boolean showPool;

	public static class HexDumpStream extends InputStream {

		private final InputStream fis;
		private int pos = 0;
		private int cnt = 0;
		private int wrap = 16;
		private byte[] str;
		private final PrintWriter out;
		private final StringBuilder tmp = new StringBuilder();
		private final boolean cleanMode;

		public HexDumpStream(boolean cleanMode, PrintWriter out, InputStream fis) {
			this.cleanMode = cleanMode;
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
			if (b == -1)
				return -1;
			if (cnt >= wrap)
			{
				if (!cleanMode)
					out.println(new String(str));
				cnt = 0;
			}
			if (cnt == 0)
			{
				if (!cleanMode)
					out.print(StringUtil.hex(pos, 8)+ " ");
			}
			if (b > 32 && b < 127)
				str[cnt] = (byte) b;
			else
				str[cnt] = '.';
			if (!cleanMode)
				out.print(StringUtil.hex(b, 2) + " ");
			cnt++;
			pos++;
			return b;
		}

		public void append(String s)
		{
			tmp.append(s);
		}
		
		public void print(Object obj) {
			if (!cleanMode)
			{
				if (cnt == 0)
					out.print("         ");
				for (int i=cnt;i<wrap;i++)
				{
					out.print("   ");
					str[i] = ' ';
				}
				out.print(new String(str) + "  ");
			}
			out.print(tmp);
			tmp.delete(0, tmp.length());
			out.println(obj);
			out.flush();
			cnt = 0;
		}
	}

	public static void main(String[] argv)
	{
		try
		{
			ByteCodeInspector bci = new ByteCodeInspector();
			List<String> args = new ArrayList<String>();
			for (String s : argv)
			{
				if (s.equals("--clean"))
					bci.cleanMode = true;
				else if (s.equals("--pool"))
					bci.showPool = true;
				else if (s.startsWith("-"))
					throw new UtilException("Unknown argument: " + s);
				else
					args.add(s);
			}
			
			if (args.size() < 1)
			{
				System.out.println("Usage: inspector <class> ...");
				return;
			}
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("dumpClass.txt"));
			for (String s : args)
			{
				if (args.size() > 1)
					writer.append(" ======= " + s + "\n");
				InputStream fis = ByteCodeInspector.class.getResourceAsStream(s);
				if (fis == null)
					throw new RuntimeException("Could not find " + s);
				bci.read(new PrintWriter(writer), fis);
				fis.close();
			}
			writer.close();
			FileUtils.cat(new File("dumpClass.txt"));
		}
		catch (FileNotFoundException ex)
		{
			ex.printStackTrace(System.out);
			System.exit(1);
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.out);
			System.exit(2);
		}
	}
		
	public void read(PrintWriter out, InputStream fis)
	{
		try
		{
			hexdump = new HexDumpStream(cleanMode, out, fis);
			DataInputStream dis = new DataInputStream(hexdump);
			int magic = dis.readInt();
			if (magic != javaMagic)
				throw new UtilException("This is not a bytecode file");
			if (!cleanMode)
				hexdump.print("Magic: " + magic);
			int minorVersion = dis.readUnsignedShort(); //minor
			int majorVersion = dis.readUnsignedShort(); // major
			if (!cleanMode)
				hexdump.print("Version: " + majorVersion + "-" + minorVersion);
			readConstantPool(dis);
			if (showPool)
				showPool();
			int access = dis.readUnsignedShort(); // access_flags
			if (!cleanMode)
				hexdump.print("Access = "  + access);
			int thisClass = dis.readUnsignedShort(); // this_class
			if (cleanMode)
			{
				String isA = "class";
				if ((access & ACC_INTERFACE) == ACC_INTERFACE)
					isA = "interface";
				else if ((access & ACC_ABSTRACT) == ACC_ABSTRACT)
					isA = "abstract class";
				hexdump.print(isA + " " + ((ClassInfo)pool[thisClass]).justName());
			}
			else
				hexdump.print("This = " + show(thisClass));
			int superClass = dis.readUnsignedShort(); // super_class
			if (cleanMode)
				hexdump.print("  extends " + ((ClassInfo)pool[superClass]).justName());
			else
				hexdump.print("Super = " + show(superClass));
			readInterfaces(dis);
			readFields(dis);
			readMethods(dis);
			readAttributes(dis, "Class");
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
		if (!cleanMode)
			hexdump.print("pool has " + poolCount + " entries, including #0");

		// This is weird offsetting ...
		for (int idx=1;idx<poolCount;idx++)
		{
			pool[idx] = readPoolEntry(dis);
			if (!cleanMode)
				hexdump.print(idx +" => " + pool[idx]);
			if (pool[idx] instanceof DoubleEntry)
			{
				if (!cleanMode)
					hexdump.print("****");
				idx++; // skip the second entry
			}
		}
	}

	private void showPool() {
		List<String> output = new ArrayList<String>();
		for (int idx=1;idx<pool.length;idx++)
			output.add(pool[idx].asClean());
		Collections.sort(output);
		for (String s : output)
		{
			hexdump.print(s);
		}
	}

	private void readInterfaces(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		if (!cleanMode)
			hexdump.print(cnt + " interfaces");
		for (int i=0;i<cnt;i++)
		{
			int idx = dis.readUnsignedShort();
			interfaces.add(idx);
			ClassInfo intf = (ClassInfo) pool[idx];
			if (cleanMode)
				hexdump.print("  implements " + intf.justName());
			else
				hexdump.print(intf);
		}
	}

	private void readFields(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		if (!cleanMode)
			hexdump.print("# of fields = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			int access = dis.readUnsignedShort(); // access_flags
			int name = dis.readUnsignedShort(); // name idx
//			System.out.println("Reading field " + pool[name]);
			int descriptor = dis.readUnsignedShort(); // descriptor idx
			hexdump.print("Field" + flags(access) + show(descriptor) + " " + show(name));
			readAttributes(dis, "Field");
		}
		
	}

	private void readMethods(DataInputStream dis) throws IOException {
		int cnt = dis.readUnsignedShort();
		if (!cleanMode)
			hexdump.print("# of methods = " + cnt);
		for (int i=0;i<cnt;i++)
		{
			int access = dis.readUnsignedShort(); // access_flags
			int name = dis.readUnsignedShort(); // name idx
			int descriptor = dis.readUnsignedShort(); // descriptor idx
			hexdump.print("Method"  + flags(access) + show(name) + " " + show(descriptor));
			readAttributes(dis, "Method");
		}
		
	}
	private String flags(int access) {
		StringBuilder sb = new StringBuilder(" ");
		if ((access & ByteCodeFile.ACC_ABSTRACT) != 0)
		{
			sb.append("abstract ");
			access &= ~ByteCodeFile.ACC_ABSTRACT;
		}
		if ((access & ByteCodeFile.ACC_FINAL) != 0)
		{
			sb.append("final ");
			access &= ~ByteCodeFile.ACC_FINAL;
		}
		if ((access & ByteCodeFile.ACC_INTERFACE) != 0)
		{
			sb.append("interface ");
			access &= ~ByteCodeFile.ACC_INTERFACE;
		}
		if ((access & ByteCodeFile.ACC_NATIVE) != 0)
		{
			sb.append("native ");
			access &= ~ByteCodeFile.ACC_NATIVE;
		}
		if ((access & ByteCodeFile.ACC_PRIVATE) != 0)
		{
			sb.append("private ");
			access &= ~ByteCodeFile.ACC_PRIVATE;
		}
		if ((access & ByteCodeFile.ACC_PROTECTED) != 0)
		{
			sb.append("protected ");
			access &= ~ByteCodeFile.ACC_PROTECTED;
		}
		if ((access & ByteCodeFile.ACC_PUBLIC) != 0)
		{
			sb.append("public ");
			access &= ~ByteCodeFile.ACC_PUBLIC;
		}
		if ((access & ByteCodeFile.ACC_STATIC) != 0)
		{
			sb.append("static ");
			access &= ~ByteCodeFile.ACC_STATIC;
		}
		if ((access & ByteCodeFile.ACC_STRICT) != 0)
		{
			sb.append("strict ");
			access &= ~ByteCodeFile.ACC_STRICT;
		}
		if ((access & ByteCodeFile.ACC_SUPER) != 0)
		{
			sb.append("super ");
			access &= ~ByteCodeFile.ACC_SUPER;
		}
		if ((access & ByteCodeFile.ACC_SYNCHRONIZED) != 0)
		{
			sb.append("synchronized ");
			access &= ~ByteCodeFile.ACC_SYNCHRONIZED;
		}
		if ((access & ByteCodeFile.ACC_TRANSIENT) != 0)
		{
			sb.append("transient ");
			access &= ~ByteCodeFile.ACC_TRANSIENT;
		}
		if ((access & ByteCodeFile.ACC_ACCESSMETH) != 0)
		{
			sb.append("access ");
			access &= ~ByteCodeFile.ACC_ACCESSMETH;
		}
		if ((access & ByteCodeFile.ACC_ENUM) != 0)
		{
			sb.append("enum ");
			access &= ~ByteCodeFile.ACC_ENUM;
		}
		
		if (access != 0)
		{
			sb.append(" !!!! [" + access + "]");
//			throw new UtilException("Unhandled flags: " + access);
		}
		return sb.toString();
	}

	private String show(int name) {
		if (cleanMode)
			return pool[name].asClean();
		return pool[name].toString();
	}

	private String show(CPInfo info) {
		if (cleanMode)
			return info.asClean();
		return info.toString();
	}
	
	private void readAttributes(DataInputStream dis, String type) throws IOException {
		int cnt = dis.readUnsignedShort();
		if (!cleanMode)
			hexdump.print(type + " attributes: " + cnt);
		for (int i=0;i<cnt;i++)
		{
			int idx = dis.readUnsignedShort(); // name_index
			int len = dis.readInt(); // length
			if (len > 5000)
				throw new RuntimeException("What? Attribute Len > 5000");
			if (!cleanMode)
				hexdump.print("idx = " + idx + ": " + pool[idx] + " len=" + len);
			if (pool[idx] == null || !(pool[idx] instanceof CPInfo.Utf8Info))
				throw new UtilException("Invalid attribute: " + idx);
			String attr = ((CPInfo.Utf8Info)pool[idx]).asString();
			if (attr.equals("Code"))
			{
				/*
				 * Code_attribute {
    	u2 max_stack;
    	u2 max_locals;
    	u4 code_length;
    	u1 code[code_length];
    	u2 exception_table_length;
    	{    	u2 start_pc;
    	      	u2 end_pc;
    	      	u2  handler_pc;
    	      	u2  catch_type;
    	}	exception_table[exception_table_length];
    	u2 attributes_count;
    	attribute_info attributes[attributes_count];
    }
				 */
				int maxStack = dis.readUnsignedShort();
				int maxLocals = dis.readUnsignedShort();
				int codeLength = dis.readInt();
				if (cleanMode)
					hexdump.append("  ");
				hexdump.print("maxStack = " + maxStack + " maxLocals = " + maxLocals + " codeLen = " + codeLength);
				if (cleanMode)
					hexdump.print("  {");
				int k=0;
				while (k<codeLength)
				{
					if (cleanMode)
						hexdump.append("    ");
					hexdump.append(StringUtil.hex(k, 4) +": ");
					k += disassemble(dis, k);
				}
				if (cleanMode)
					hexdump.print("  }");
				int excLength = dis.readUnsignedShort();
				if (!cleanMode)
					hexdump.print(excLength + " exceptions");
				for (int j=0;j<excLength;j++)
				{
					/* int start_pc = */ dis.readUnsignedShort();
					/* int end_pc = */ dis.readUnsignedShort();
					/* int handler_pc = */ dis.readUnsignedShort();
					/* int catch_type = */ dis.readUnsignedShort();
					hexdump.print("");
				}
				readAttributes(dis, "Code");
			}
			else if (attr.equals("Signature") || attr.equals("SourceFile"))
			{
				if (len != 2)
					throw new UtilException("Attribute has incorrect length: " + attr + ": length = " + len);
				int ai = dis.readUnsignedShort();
				hexdump.print("[" + type +" " + attr + "]: " + show(ai));
			}
			else if (attr.equals("Exceptions"))
			{
				byte[] data = new byte[len];
				readBytes(dis, data);
				for (int j=2;j<data.length;j+=2)
				{
					int ex = getDataShort(data, j);
					hexdump.print("throws exception " + pool[ex].asClean());
				}
			}
			else if (attr.equals("RuntimeVisibleAnnotations")) {
				int acnt = dis.readUnsignedShort();
				hexdump.print("Has " + acnt + " annotations");
				for (int rva = 0;rva<acnt;rva++)
				{
					readAnnotation(dis);
				}
			}
			else if (attr.equals("RuntimeVisibleParameterAnnotations")) {
				int nparams = dis.readUnsignedByte();
				hexdump.print("Method has " + nparams + " parameters");
				for (int np = 0;np<nparams;np++)
				{
					int acnt = dis.readUnsignedShort();
					hexdump.print("Parameter " + np + " has " + acnt + " annotations");
					for (int rva = 0;rva<acnt;rva++)
					{
						readAnnotation(dis);
					}
				}
			}
			else if (attr.equals("InnerClasses")) {
				int refersTo = dis.readUnsignedShort();
				List<String> output = new ArrayList<String>();
				for (int j=0;j<refersTo;j++)
				{
					int ic = dis.readUnsignedShort();
					int oc = dis.readUnsignedShort();
					int in = dis.readUnsignedShort();
					int acc = dis.readUnsignedShort();
					String out = "Ref" + flags(acc) + show(ic) + " <= " + show(oc) + "." + show(in);
					if (cleanMode)
						output.add(out);
					else
						hexdump.print(out);
				}
				if (cleanMode)
				{
					Collections.sort(output);
					for (String s : output)
						hexdump.print(s);
				}
			}
			else
			{
				byte[] bytes = new byte[len];
				readBytes(dis, bytes);
				if (cleanMode)
					hexdump.print(type + " attribute " + pool[idx].asClean() + "[" + len + "]");
				else
					hexdump.print("");
			}
		}		
	}

	private void readAnnotation(DataInputStream dis) throws IOException {
		int a = dis.readUnsignedShort();
		hexdump.print("@" + pool[a].asClean());
		int nvp = dis.readUnsignedShort();
		hexdump.print(nvp + " arguments");
		for (int rvp=0;rvp<nvp;rvp++)
		{
			int n = dis.readUnsignedShort();
			hexdump.append("  " + pool[n].asClean() + "=");
			readElementValue(dis);
		}
	}

	private void readElementValue(DataInputStream dis) throws IOException {
		char tag = (char) dis.readUnsignedByte();
		switch (tag) {
		case 'c': // class
		{
			int offset = dis.readUnsignedShort();
			hexdump.print(pool[offset].asClean() + ".class");
			break;
		}
		case 's': // utf8
		{
			int offset = dis.readUnsignedShort();
			hexdump.print('"' + pool[offset].asClean() + '"');
			break;
		}
		case '[': // array
		{
			int arrSize = dis.readUnsignedShort();
			hexdump.print("[");
			for (int ai = 0;ai<arrSize;ai++)
				readElementValue(dis);
			hexdump.print("]");
			break;
		}
		case '@': // attribute
		{
			readAnnotation(dis);
			break;
		}
		default:
			throw new UtilException("The attribute value tag " + tag + " is not supported");
		}
	}

	private int getDataShort(byte[] data, int j) {
		int msb = ((int)data[j])&0xff;
		int lsb = ((int)data[j+1])&0xff;
		return msb << 8 | lsb;
	}

	private int disassemble(DataInputStream dis, int offset) throws IOException {
		int opcode = dis.readUnsignedByte();
		switch (opcode)
		{
		case 0x01:
		{
			hexdump.print("aconst_null");
			return 1;
		}
		case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07: case 0x08:
		{
			hexdump.print("iconst_" + (opcode-0x03));
			return 1;
		}
		case 0x10:
		{
			int bi = dis.readByte();
			hexdump.print("bipush " + bi);
			return 2;
		}
		case 0x12:
		{
			int idx = dis.readUnsignedByte();
			hexdump.print("ldc " + show(idx));
			return 2;
		}
		case 0x13:
		{
			int idx = dis.readUnsignedShort();
			hexdump.print("ldc_w " + show(idx));
			return 3;
		}
		case 0x15:
		{
			int reg = dis.readUnsignedByte();
			hexdump.print("iload " + reg);
			return 2;
		}
		case 0x18:
		{
			int reg = dis.readUnsignedByte();
			hexdump.print("dload " + reg);
			return 2;
		}
		case 0x19:
		{
			int reg = dis.readUnsignedByte();
			hexdump.print("aload "+ reg);
			return 2;
		}
		case 0x1a: case 0x1b: case 0x1c: case 0x1d:
		{
			hexdump.print("iload_"+(opcode-0x1a));
			return 1;
		}
		case 0x26: case 0x27: case 0x28: case 0x29:
		{
			hexdump.print("dload_"+(opcode-0x26));
			return 1;
		}
		case 0x2a: case 0x2b: case 0x2c: case 0x2d:
		{
			hexdump.print("aload_"+(opcode-0x2a));
			return 1;
		}
		case 0x36:
		{
			int reg = dis.readUnsignedByte();
			hexdump.print("istore " + reg);
			return 2;
		}
		case 0x3a:
		{
			int reg = dis.readUnsignedByte();
			hexdump.print("astore "+ reg);
			return 2;
		}
		case 0x4b: case 0x4c: case 0x4d: case 0x4e:
		{
			hexdump.print("astore_"+(opcode-0x4b));
			return 1;
		}
		case 0x57:
		{
			hexdump.print("pop");
			return 1;
		}
		case 0x58:
		{
			hexdump.print("pop2");
			return 1;
		}
		case 0x59:
		{
			hexdump.print("dup");
			return 1;
		}
		case 0x5c:
		{
			hexdump.print("dup2");
			return 1;
		}
		case 0x60:
		{
			hexdump.print("iadd");
			return 1;
		}
		case 0x99: case 0x9a: case 0x9b: case 0x9c: case 0x9d: case 0x9e:
		{
			String op = "if" + new String[] { "eq", "ne", "lt", "ge", "gt", "le" }[opcode-0x99];
			short jumpTo = dis.readShort();
			hexdump.print(op + " " + StringUtil.hex(offset+jumpTo, 4));
			return 3;
		}
		case 0x9f: case 0xa0: case 0xa1: case 0xa2: case 0xa3: case 0xa4:
		{
			String op = "if_icmp" + new String[] { "eq", "ne", "lt", "ge", "gt", "le" }[opcode-0x9f];
			short jumpTo = dis.readShort();
			hexdump.print(op + " " + StringUtil.hex(offset+jumpTo, 4));
			return 3;
		}
		case 0xa7:
		{
			short jumpTo = dis.readShort();
			hexdump.print("goto " + StringUtil.hex(offset+jumpTo, 4));
			return 3;
		}
		case 0xac:
		{
			hexdump.print("ireturn");
			return 1;
		}
		case 0xaf:
		{
			hexdump.print("dreturn");
			return 1;
		}
		case 0xb0:
		{
			hexdump.print("areturn");
			return 1;
		}
		case 0xb1:
		{
			hexdump.print("return");
			return 1;
		}
		case 0xb2:
		{
			int idx = dis.readUnsignedShort();
			hexdump.print("getstatic " + show(idx));
			return 3;
		}
		case 0xb3:
		{
			int idx = dis.readUnsignedShort();
			hexdump.print("putstatic " + show(idx));
			return 3;
		}
		case 0xb4:
		{
			int idx = dis.readUnsignedShort();
			hexdump.print("getfield " + show(idx));
			return 3;
		}
		case 0xb5:
		{
			int idx = dis.readUnsignedShort();
			hexdump.print("putfield " + show(idx));
			return 3;
		}
		case 0xb6:
		{
			int idx = dis.readUnsignedShort();
			CPInfo info = pool[idx];
			hexdump.print("invokevirtual " + show(info));
			return 3;
		}
		case 0xb7:
		{
			int idx = dis.readUnsignedShort();
			CPInfo info = pool[idx];
			hexdump.print("invokespecial " + show(info));
			return 3;
		}
		case 0xb8:
		{
			int idx = dis.readUnsignedShort();
			CPInfo info = pool[idx];
			hexdump.print("invokestatic " + show(info));
			return 3;
		}
		case 0xb9:
		{
			int idx = dis.readUnsignedShort();
			CPInfo info = pool[idx];
			int count = dis.readUnsignedByte();
			dis.readUnsignedByte(); // always zero
			hexdump.print("invokeinterface " + show(info) + ", " + count);
			return 5;
		}
		case 0xbb:
		{
			int idx = dis.readUnsignedShort();
			CPInfo info = pool[idx];
			hexdump.print("new " + show(info));
			return 3;
		}
		case 0xbd:
		{
			int idx = dis.readUnsignedShort();
			CPInfo info = pool[idx];
			hexdump.print("anewarray " + show(info));
			return 3;
		}
		case 0xbf:
		{
			hexdump.print("athrow");
			return 1;
		}
		case 0xc0:
		{
			int idx = dis.readUnsignedShort();
			CPInfo info = pool[idx];
			hexdump.print("checkcast " + show(info));
			return 3;
		}
		case 0xc6:
		{
			short jumpTo = dis.readShort();
			hexdump.print("ifnull " + StringUtil.hex(offset+jumpTo, 4));
			return 3;
		}
		case 0xc7:
		{
			short jumpTo = dis.readShort();
			hexdump.print("ifnonnull " + StringUtil.hex(offset+jumpTo, 4));
			return 3;
		}
		default:
			throw new UtilException("Invalid opcode " + StringUtil.hex(opcode, 2));
//			return 1;
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
