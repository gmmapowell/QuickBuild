package com.gmmapowell.jsgen;

import com.gmmapowell.exceptions.UtilException;

public class JSFunction implements JSEntry {
	private String name;
	private final JSVar[] args;
	private final JSBlock block;
	private final JSScope scope;

	JSFunction(JSScope parent, String... args)
	{
		scope = new JSScope(parent);
		block = new JSBlock(scope);
		this.args = new JSVar[args.length];
		for (int i=0;i<args.length;i++)
			this.args[i] = scope.getExactVar(args[i]);
	}

	public void giveName(String name) {
		this.name = name;
	}

	public JSVar getArg(String a) {
		for (JSVar ai : args)
			if (ai.getName().equals(a))
				return ai;
		throw new UtilException("There is no argument " + a);
	}

	public JSBlock getBlock() {
		return block;
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
		for (JSVar a : args) {
			sb.append(sep);
			sb.append(a.getName());
			sep = ", ";
		}
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
