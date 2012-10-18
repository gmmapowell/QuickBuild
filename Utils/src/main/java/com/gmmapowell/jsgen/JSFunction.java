package com.gmmapowell.jsgen;

import com.gmmapowell.exceptions.UtilException;

public class JSFunction implements JSEntry {
	private final String field;
	private final String[] args;
	private final JSBlock block = new JSBlock();

	public JSFunction(String field, String... args)
	{
		this.field = field;
		this.args = args;
	}

	public Var arg(String a) {
		for (String ai : args)
			if (ai.equals(a))
				return new Var(a);
		throw new UtilException("There is no argument " + a);
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.append("\"" + field +"\":\"function(");
		appendArgs(sb);
		sb.append(")");
		getBlock().asJson(sb);
		sb.append("\"");
	}

	private void appendArgs(JSBuilder sb) {
		String sep = "";
		for (String a : args) {
			sb.append(sep);
			sb.append(a);
			sep = ", ";
		}
	}
	
	public JSBlock getBlock() {
		return block;
	}

	public void textCode(String code) {
		getBlock().add(new TextCode(code));
	}

}
