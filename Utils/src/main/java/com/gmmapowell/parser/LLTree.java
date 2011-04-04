package com.gmmapowell.parser;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LLGrammar.Production;
import com.gmmapowell.utils.PrettyPrinter;

public class LLTree {

	private final Production production;
	private final List<Object> nts;

	public LLTree(Production p, List<Object> nts) {
		this.production = p;
		this.nts = nts;
	}

	@Override
	public String toString() {
		PrettyPrinter pp = new PrettyPrinter();
		prettyPrint(pp);
		return pp.toString();
	}

	private void prettyPrint(PrettyPrinter pp) {
		pp.requireNewline();
		pp.append(production);
		pp.indentMore();
		for (Object o : nts)
		{
			pp.requireNewline();
			if (o instanceof LLToken)
				pp.append(o);
			else if (o instanceof LLTree)
				((LLTree)o).prettyPrint(pp);
		}
		pp.indentLess();
		pp.requireNewline();
	}

	public Iterable<LLTree> items(String string, int... route) {
		List<LLTree> ret = new ArrayList<LLTree>();
		followRoute(route.length, route).findItems(ret, string);
		return ret;
	}

	private void findItems(List<LLTree> ret, String string) {
		if (production.result().equals(string))
		{
			ret.add(this);
			return;
		}
		for (Object nt : nts)
		{
			if (nt instanceof LLTree)
				((LLTree)nt).findItems(ret, string);
		}
	}

	public LLToken route(String string, int... route) {
		int k = route.length-1;
		LLTree curr = followRoute(k, route);
		if (route[k] >= curr.nts.size())
			throw new UtilException("Node does not have " + route[k] + " items");
		Object o = curr.nts.get(route[k]);
		if (!(o instanceof LLToken))
			throw new UtilException("Node " + k + " is not a token");
		LLToken ret = (LLToken)o;
		if (string != null && !ret.tag().equals(string))
			throw new UtilException("Node did not have tag " + string + " but " + ret.tag());
		return ret;
	}

	private LLTree followRoute(int k, int... route) {
		LLTree curr = this;
		for (int i=0;i<k;i++)
		{
			if (route[i] >= curr.nts.size())
				throw new UtilException("Node does not have " + route[i] + " items");
			Object o = curr.nts.get(route[i]);
			if (!(o instanceof LLTree))
				throw new UtilException("Node " + i + " is not a tree");
			curr = (LLTree)o;
		}
		return curr;
	}
}
