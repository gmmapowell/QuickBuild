package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gmmapowell.bytecode.ByteCodeInspector.HexDumpStream;
import com.gmmapowell.bytecode.CPInfo.DoubleEntry;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class ConstPool {
	private CPInfo[] pool;
	private int nextPoolEntry;

	public ConstPool() {
		pool = new CPInfo[10];
		nextPoolEntry = 1;
	}
	
	public ConstPool(int poolCount) {
		pool = new CPInfo[poolCount];
		nextPoolEntry = poolCount;
	}

	public CPInfo get(int idx) {
		return pool[idx];
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

	void showPool(HexDumpStream hexdump) {
		List<String> output = new ArrayList<String>();
		for (int idx=1;idx<pool.length;idx++)
			output.add(pool[idx].asClean());
		Collections.sort(output);
		for (String s : output)
		{
			hexdump.print(s);
		}
	}

	public void setPoolEntry(int idx, CPInfo readPoolEntry) {
		pool[idx] = readPoolEntry;
	}

	public void writeConstantPool(DataOutputStream dos) throws IOException {
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
	
	int requireClass(String string) {
		String s = FileUtils.convertDottedToSlashPath(string);
		int utf8Idx = 0;
		for (int i=1;i<nextPoolEntry;i++)
			if (get(i) != null && get(i) instanceof CPInfo.Utf8Info && ((CPInfo.Utf8Info)get(i)).asString().equals(s))
			{
				utf8Idx = i;
				break;
			}
		if (utf8Idx > 0)
		{
			for (int i=1;i<nextPoolEntry;i++)
				if (get(i) != null && get(i) instanceof CPInfo.ClassInfo && ((CPInfo.ClassInfo)get(i)).idx == utf8Idx)
					return i;
		}
		else
			utf8Idx = nextPoolEntry+1;
		int clzIdx = nextPoolEntry;
		addPoolEntry(new CPInfo.ClassInfo(this, utf8Idx));
		addPoolEntry(new CPInfo.Utf8Info(s));
		return clzIdx;
	}

	public short requireUtf8(String name) {
		if (name == null)
			throw new UtilException("Cannot have null name");
		for (short i=1;i<nextPoolEntry;i++)
			if (get(i) != null && get(i) instanceof CPInfo.Utf8Info && ((CPInfo.Utf8Info)get(i)).asString().equals(name))
			{
				return i;
			}
		return addPoolEntry(new CPInfo.Utf8Info(name));
	}
	
	public int requireNT(int methIdx, int sigIdx) {
		for (short i=1;i<nextPoolEntry;i++)
			if (get(i) != null && get(i) instanceof CPInfo.NTInfo)
			{
				CPInfo.NTInfo nt = (CPInfo.NTInfo)get(i);
				if (nt.isA(methIdx, sigIdx))
					return i;
			}
		return addPoolEntry(new CPInfo.NTInfo(this, methIdx, sigIdx));
	}

	public int requireRef(int refType, int clzIdx, int ntIdx) {
		for (short i=1;i<nextPoolEntry;i++)
			if (get(i) != null && get(i) instanceof CPInfo.RefInfo)
			{
				CPInfo.RefInfo r = (CPInfo.RefInfo)get(i);
				if (r.isA(refType, clzIdx, ntIdx))
					return i;
			}
		return addPoolEntry(new CPInfo.RefInfo(this, clzIdx, ntIdx, refType));
	}

	public int requireString(String s) {
		int utf8Idx = 0;
		for (int i=1;i<nextPoolEntry;i++)
			if (get(i) != null && get(i) instanceof CPInfo.Utf8Info && ((CPInfo.Utf8Info)get(i)).asString().equals(s))
			{
				utf8Idx = i;
				break;
			}
		if (utf8Idx > 0)
		{
			for (int i=1;i<nextPoolEntry;i++)
				if (get(i) != null && get(i) instanceof CPInfo.StringInfo && ((CPInfo.StringInfo)get(i)).idx == utf8Idx)
					return i;
		}
		else
			utf8Idx = nextPoolEntry+1;
		int strIdx = nextPoolEntry;
		addPoolEntry(new CPInfo.StringInfo(this, utf8Idx));
		addPoolEntry(new CPInfo.Utf8Info(s));
		if (strIdx == 0)
			throw new UtilException("StrIdx == 0");
		return strIdx;
	}

	public int size() {
		return nextPoolEntry;
	}
}
