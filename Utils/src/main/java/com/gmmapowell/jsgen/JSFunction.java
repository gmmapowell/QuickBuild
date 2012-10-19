package com.gmmapowell.jsgen;

import com.gmmapowell.exceptions.UtilException;

public class JSFunction implements JSEntry {
	private String name;
	private final String[] args;
	private final JSBlock block = new JSBlock();

	public JSFunction(String... args)
	{
		this.args = args;
	}

	public void giveName(String name) {
		this.name = name;
	}
	
	public JSVar arg(String a) {
		for (String ai : args)
			if (ai.equals(a))
				return new JSVar(a);
		throw new UtilException("There is no argument " + a);
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.ident("function");
		if (name != null)
			sb.ident(name);
		sb.orb();
		appendArgs(sb);
		sb.crb();
		block.toScript(sb);
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

	@Override
	public String toString() {
		JSBuilder sb = new JSBuilder();
        toScript(sb);
		return sb.toString();
	}
	
	@Deprecated
	public void textCode(String code) {
		getBlock().add(new TextCode(code));
	}
}
