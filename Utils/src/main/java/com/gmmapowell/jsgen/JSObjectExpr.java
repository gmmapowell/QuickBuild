package com.gmmapowell.jsgen;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;

public class JSObjectExpr extends JSExpr {
	private Map<String, JSEntry> members = new LinkedHashMap<String, JSEntry>();
	private final JSScope scope;
	
	JSObjectExpr(JSScope scope) {
		this.scope = scope;
	}

	public JSFunction method(String name, String... args) {
		if (members.containsKey(name))
			throw new UtilException("Cannot define duplicate member " + name);
		JSFunction ret = new JSFunction(scope, args);
		members.put(name, ret);
		return ret;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.ocb();
		for (Map.Entry<String, JSEntry> kv : members.entrySet()) {
			sb.fieldName(kv.getKey());
			kv.getValue().toScript(sb);
		}
		sb.ccb();
	}

}