package com.gmmapowell.utils;

public class ArgumentDefinition {
	private final String argument;
	private final Cardinality cardinality;
	private final String toVar;
	private final String msg;

	public ArgumentDefinition(String argument, Cardinality cardinality, String toVar, String msg) {
		this.argument = argument;
		this.cardinality = cardinality;
		this.toVar = toVar;
		this.msg = msg;
	}
}
