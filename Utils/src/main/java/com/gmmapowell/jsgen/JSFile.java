package com.gmmapowell.jsgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.collections.CollectionUtils;

public class JSFile {
	private final JSScope scope = new JSScope(null);
	private final JSBlock block = new JSBlock(scope, false);
	private final Map<String, JSNamespace> namespaces = new HashMap<String, JSNamespace>();
	
	public JSBlock getBlock() {
		return block;
	}

	public JSScope getScope() {
		return scope;
	}
	
	public JSFunction createFunction(String name, String... args) {
		JSFunction ret = newFunction(args);
		ret.giveName(name);
		block.add(ret);
		return ret;
	}

	public JSFunction newFunction(String... args) {
		return newFunction(CollectionUtils.listOf(args));
	}

	private JSFunction newFunction(List<String> args) {
		JSFunction ret = new JSFunction(scope, args);
		return ret;
	}

	public JSEntry newSymbol(String code) {
		return scope.getExactVar(code);
	}
	
	public JSNamespace namespace(String s) {
		if (namespaces.containsKey(s))
			return namespaces.get(s);
		JSNamespace ret = new JSNamespace(s);
		namespaces.put(s, ret);
		return ret;
	}

	public void toScript(JSBuilder jsb) {
		block.toScript(jsb);
	}

	@Override
	public String toString() {
		JSBuilder sb = new JSBuilder();
		sb.setPretty(true);
		block.toScript(sb);
		return sb.toString();
	}
}
