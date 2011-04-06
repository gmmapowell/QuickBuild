package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class CPInfo {
	public interface DoubleEntry {

	}

	private final CPInfo[] pool;
	protected final int idx;

	public CPInfo()
	{
		pool = null;
		idx = 0;
	}
	
	public CPInfo(CPInfo[] pool, int idx) {
		this.pool = pool;
		this.idx = idx;
	}

	public static class Utf8Info extends CPInfo {
		private final String utf8;

		public Utf8Info(byte[] bytes) {
			utf8 = new String(bytes);
		}

		public Utf8Info(String str) {
			utf8 = str;
		}

		@Override
		public String toString() {
			return "U8: " + utf8;
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
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_Double);
			dos.writeDouble(val);
		}
	}

	public static class ClassInfo extends CPInfo {

		public ClassInfo(CPInfo[] pool, int idx) {
			super(pool, idx);
		}
		
		@Override
		public String toString() {
			return "ClassInfo[" + idx + "]";
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassInfo)
			{
				return ((ClassInfo)obj).idx == idx;
			}
			else if (obj instanceof String)
			{
				return ((Utf8Info)super.pool[idx]).utf8.equals(obj);
			}
			// TODO Auto-generated method stub
			return super.equals(obj);
		}

		@Override
		public void writeEntry(DataOutputStream dos) throws IOException {
			dos.write(ByteCodeFile.CONSTANT_Class);
			dos.writeShort(idx);
		}

	}

	public static class StringInfo extends CPInfo {

		public StringInfo(CPInfo[] pool, int idx) {
			super(pool, idx);
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

		public RefInfo(CPInfo[] pool, int clz, int nt, int tag) {
			super(pool, 0);
			this.clz = clz;
			this.nt = nt;
			this.tag = tag;
		}


		@Override
		public String toString() {
			String s = getClass().getSimpleName() + "[" + super.hex(clz) + "," + super.hex(nt) + "," + tag + "] > ";
//			s += "{" +super.pool[clz] + "} [" + super.pool[nt] + "]";
//			if (super.pool != null && super.pool[super.idx] != null)
//				return + idx + "/" + Integer.toHexString(idx) + "]> " + pool[idx].toString();
			return s;
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

		public NTInfo(CPInfo[] pool, int name, int descriptor) {
			super(pool, 0);
			this.name = name;
			this.descriptor = descriptor;
		}

		@Override
		public String toString() {
			String s = getClass().getSimpleName() + "[" + super.hex(name) + "," + super.hex(descriptor) + "]"; 
//			if (super.pool != null && super.pool[super.idx] != null)
//				return + idx + "/" + Integer.toHexString(idx) + "]> " + pool[idx].toString();
			return s;
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
		if (pool != null && pool[idx] != null)
			return getClass().getSimpleName() + "[" + hex(idx) + "]> " + pool[idx].toString();
		return super.toString();
	}

	public abstract void writeEntry(DataOutputStream dos) throws IOException;
}
