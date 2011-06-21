package com.gmmapowell.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public class Instruction {
	private final int[] bytes;

	public Instruction(int... bytes) {
		this.bytes = bytes;
	}

	public int length() {
		return bytes.length;
	}

	public void write(DataOutputStream dos) throws IOException {
		for (int b : bytes)
			dos.writeByte(b);
	}

	@Override
	public String toString() {
		return Integer.toHexString(bytes[0]&0xff);
	}

	public void setLocation(int offset, int count) {
		bytes[offset] = (count>>8)&0xff;
		bytes[offset+1] = (count)&0xff;
	}
}
