package com.gmmapowell.utils;

public class ArgumentDefinition {
	final String text;
	final Cardinality cardinality;
	final String toVar;
	final String message;

	public ArgumentDefinition(String argument, Cardinality cardinality, String toVar, String msg) {
		this.text = argument;
		this.cardinality = cardinality;
		this.toVar = toVar;
		this.message = msg;
	}
}
