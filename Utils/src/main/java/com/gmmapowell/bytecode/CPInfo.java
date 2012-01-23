package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import com.gmmapowell.exceptions.UtilException;

public abstract class CPInfo {
	public interface DoubleEntry {

	}

	protected final ConstPool pool;
	protected final int idx;

	public CPInfo()
	{
		pool = null;
		idx = 0;
	}
	
	public CPInfo(ConstPool pool, int idx) {
		this.pool = pool;
		this.idx = idx;
	}

	public static class Utf8Info extends CPInfo {
		private final String utf8;

		public Utf8Info(byte[] bytes) {
			utf8 = new String(bytes);
		}

		public Utf8Info(String str) {
			if (str == null)
				throw new UtilException("Cannot create UTF8 with null string");
			utf8 = str;
		}

		@Override
		public String toString() {
			return "U8: " + utf8;
		}

		@Override
		public String asClean() {
			return utf8;
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_UTF8);
			dos.writeShort(utf8.length());
			dos.writeBytes(utf8);
		}

		public String asString() {
			return utf8;
		}
	}

	public static class IntegerInfo extends CPInfo {

		private final int val;

		public IntegerInfo(int i) {
			this.val = i;
		}

		@Override
		public String toString() {
			return "Integer: " + val;
		}

		@Override
		public String asClean() {
			return Integer.toString(val);
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_Integer);
			dos.writeInt(val);
		}
	}

	public static class FloatInfo extends CPInfo {
		private float val;

		public FloatInfo(float f) {
			val = f;
		}

		@Override
		public String toString() {
			return "Float: " + val;
		}

		@Override
		public String asClean() {
			return Float.toString(val);
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_Float);
			dos.writeFloat(val);
		}
	}

	public static class LongInfo extends CPInfo implements DoubleEntry {
		private long val;

		public LongInfo(int high, int low) {
			long l = high;
			val = (l << 32) | low;
		}

		@Override
		public String toString() {
			return "Long: " + val;
		}

		@Override
		public String asClean() {
			return Long.toString(val);
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_Long);
			dos.writeInt((int)(val >> 32));
			dos.writeInt((int)(val & 0xfffffffL));
		}
	}


	public static class DoubleInfo extends CPInfo implements DoubleEntry {
		private double val;

		public DoubleInfo(double d) {
			val = d;
		}

		@Override
		public String toString() {
			return "Double: " + val;
		}

		@Override
		public String asClean() {
			return Double.toString(val);
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_Double);
			dos.writeDouble(val);
		}
	}

	public static class ClassInfo extends CPInfo {

		public ClassInfo(ConstPool pool, int idx) {
			super(pool, idx);
		}
		
		@Override
		public String toString() {
			return "ClassInfo[" + idx + "{" + pool.get(idx) + "}]";
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassInfo)
			{
				return ((ClassInfo)obj).idx == idx;
			}
			else if (obj instanceof String)
			{
				return ((Utf8Info)super.pool.get(idx)).utf8.equals(obj);
			}
			return super.equals(obj);
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_Class);
			dos.writeShort(idx);
		}

		@Override
		public String asClean() {
			return "Class["+justName()+"]";
		}

		public String justName() {
			return ((Utf8Info)pool.get(idx)).asClean();
		}

	}

	public static class StringInfo extends CPInfo {

		public StringInfo(ConstPool pool, int idx) {
			super(pool, idx);
		}

		@Override
		public String asClean() {
			return '"' + pool.get(idx).asClean() + '"';
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_String);
			dos.writeShort(idx);
		}

	}


	public static class RefInfo extends CPInfo {

		private final int tag;
		private final int clz;
		private final int nt;

		public RefInfo(ConstPool pool, int clz, int nt, int tag) {
			super(pool, 0);
			this.clz = clz;
			this.nt = nt;
			this.tag = tag;
		}

		@Override
		public String asClean() {
			if (tag == 9)
			{
				NTInfo cpInfo = (NTInfo) pool.get(nt);
				return "F" + pool.get(clz).asClean() + "." + pool.get(cpInfo.name).asClean() + "{" + pool.get(cpInfo.descriptor).asClean() + "}";  
			}
			else if (tag == 10 || tag == 11)
			{
				String cm = "M";
				if (tag == 11)
					cm = "I";
				NTInfo cpInfo = (NTInfo) pool.get(nt);
				return cm + pool.get(clz).asClean() + "." + pool.get(cpInfo.name).asClean() + " " + pool.get(cpInfo.descriptor).asClean(); 
			}
			else
				throw new UtilException("No");
		}

		@Override
		public String toString() {
			String s = tagAsString() + getClass().getSimpleName() + "[" + super.hex(clz) + "," + super.hex(nt) + "] {" + pool.get(clz) + " " + pool.get(nt) + "}";
//			s += "{" +super.pool.get(clz) + "} [" + super.pool.get(nt) + "]";
//			if (super.pool != null && super.pool.get(super.idx) != null)
//				return + idx + "/" + Integer.toHexString(idx) + "]> " + pool.get(idx).toString();
			return s;
		}


		private String tagAsString() {
			if (tag == 9)
				return "Field";
			else if (tag == 10)
				return "Method";
			else if (tag == 11)
				return "Interface";
			else
				throw new UtilException("Unspecified tag " + tag); 
		}


		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(tag);
			dos.writeShort(clz);
			dos.writeShort(nt);
		}


		public boolean isA(int refType, int clzIdx, int sigIdx) {
			return this.tag == refType && this.clz == clzIdx && this.nt == sigIdx;
		}
	}

	public static class NTInfo extends CPInfo {

		private final int name;
		private final int descriptor;

		public NTInfo(ConstPool pool, int name, int descriptor) {
			super(pool, 0);
			this.name = name;
			this.descriptor = descriptor;
		}

		@Override
		public String toString() {
			String s = getClass().getSimpleName() + "[" + super.hex(name) + "," + super.hex(descriptor) + "] {" + pool.get(name) + " " + pool.get(descriptor) + "}"; 
//			if (super.pool != null && super.pool.get(super.idx) != null)
//				return + idx + "/" + Integer.toHexString(idx) + "]> " + pool.get(idx).toString();
			return s;
		}

		@Override
		public String asClean() {
			return toString();
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_NameAndType);
			dos.writeShort(name);
			dos.writeShort(descriptor);
		}

		public boolean isA(int methIdx, int sigIdx) {
			return this.name == methIdx && this.descriptor == sigIdx;
		}

	}
	
	
	private String hex(int val) {
		return val + "/#" + Integer.toHexString(val);
	}

	@Override
	public String toString() {
		if (pool != null && pool.get(idx) != null)
			return getClass().getSimpleName() + "[" + hex(idx) + "] " + pool.get(idx).toString();
		return super.toString();
	}

	public abstract void writeEntry(DataOutputStream dos) throws IOException;

	public abstract String asClean();
}
