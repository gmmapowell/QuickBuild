package com.gmmapowell.bytecode;

import java.util.List;

public class Marker {
	private final List<Instruction> instructions;
	private final int offset;
	private final int pointer;

	public Marker(List<Instruction> instructions, int offset) {
		this.instructions = instructions;
		this.pointer = instructions.size();
		this.offset = offset;
	}

	/**
	 * This should be used for forward jumps.  It assumes that the jump instruction
	 * is already present.  We need another one for backward jumps.
	 */
	public void setHere() {
		int count = 0;
		int k = 0;
		for (Instruction i : instructions)
			if (k++ >= pointer)
				count += i.length();
		Instruction jump = instructions.get(pointer);
		jump.setLocation(offset, count);
	}

}
