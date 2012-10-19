package com.gmmapowell.jsgen;

import java.util.ArrayList;
import java.util.List;

public class JSListExpr extends JSExpr {
	private final List<JSExpr> members = new ArrayList<JSExpr>();
	
	public void add(JSExpr expr) {
		members.add(expr);
	}

	// Copy the list, but keep the items the same
	public JSListExpr shallowClone() {
		JSListExpr ret = new JSListExpr();
		for (JSExpr e : members)
			ret.add(e);
		return ret;
	}

	@Override
	public void toScript(JSBuilder sb) {
		sb.osb();
		String sep = "";
		for (JSExpr e : members)
		{
			sb.append(sep);
			e.toScript(sb);
			sep = ",";
		}
		sb.csb();
	}
}