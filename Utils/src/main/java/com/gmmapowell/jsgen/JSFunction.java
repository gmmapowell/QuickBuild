package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

public class JSFunction extends JSExpr {
	private String name;
	private final List<JSVar> args = new ArrayList<JSVar>();
	private final JSBlock block;
	private final JSScope scope;

	JSFunction(JSScope parent, List<String> args)
	{
		scope = new JSScope(parent);
		block = new JSBlock(scope);
		for (String a : args)
			this.args.add(scope.getExactVar(a));
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
}
