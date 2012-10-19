package com.gmmapowell.jsgen;

public class JSMethod implements JSEntry {
	private final String field;
	private final JSFunction fn;

	public JSMethod(String field, String... args)
	{
		this.field = field;
		this.fn = new JSFunction(args);
	}

	public JSVar arg(String a) {
		return fn.arg(a);
	}

	public JSBlock getBlock() {
		return fn.getBlock();
	}

	@Deprecated
	public void textCode(String code) {
		fn.textCode(code);
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.fieldName(field);
		fn.toScript(sb);
	}
}
