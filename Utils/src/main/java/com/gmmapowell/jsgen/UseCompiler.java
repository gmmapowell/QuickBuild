package com.gmmapowell.jsgen;


public abstract class UseCompiler extends JSCompiler {
	public UseCompiler(JSCompiler jsc) {
		super(jsc.getBlock());
	}
}