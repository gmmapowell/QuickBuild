package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.collections.CollectionUtils;

public class JSFile {
	private final List<JSEntry> entries = new ArrayList<JSEntry>();
	private final JSScope scope = new JSScope(null);
	private final Map<String, JSNamespace> namespaces = new HashMap<String, JSNamespace>();
	
	public JSFunction newFunction(String... args) {
		return newFunction(CollectionUtils.listOf(args));
	}

	private JSFunction newFunction(List<String> args) {
		JSFunction ret = new JSFunction(scope, args);
		entries.add(ret);
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

	@Override
	public String toString() {
		JSBuilder sb = new JSBuilder();
		sb.setPretty(true);
		for (JSEntry e : entries)
			e.toScript(sb);
		return sb.toString();
	}
}
