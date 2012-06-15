package com.gmmapowell.http;

public class GPFrame {
	final int opcode;
	final byte[] data;

	public GPFrame(int opcode, byte[] data) {
		this.opcode = opcode;
		this.data = data;
	}

}
