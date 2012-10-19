package com.gmmapowell.jsgen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.exceptions.UtilException;

public class JSObjectExpr extends JSExpr {
	private Map<String, JSEntry> members = new LinkedHashMap<String, JSEntry>();
	private final JSScope scope;
	
	JSObjectExpr(JSScope scope) {
		this.scope = scope;
	}

	public JSFunction method(String name, String... args) {
		return method(name, CollectionUtils.listOf(args));
	}

	public JSFunction method(String name, List<String> args) {
		if (members.containsKey(name))
			throw new UtilException("Cannot define duplicate member " + name);
		JSFunction ret = new JSFunction(scope, args);
		members.put(name, ret);
		return ret;
	}

	public void var(String name, JSExpr expr) {
		if (members.containsKey(name))
			throw new UtilException("Cannot define duplicate member " + name);
		members.put(name, expr);
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.ocb();
		for (Map.Entry<String, JSEntry> kv : members.entrySet()) {
			sb.fieldName(kv.getKey());
			kv.getValue().toScript(sb);
			sb.objectComma();
		}
		sb.ccb();
	}
}