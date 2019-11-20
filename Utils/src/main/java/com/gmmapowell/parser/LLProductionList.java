package com.gmmapowell.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.parser.LLGrammar.Production;
import com.gmmapowell.parser.LLGrammar.ProductionElement;
import com.gmmapowell.utils.PrettyPrinter;

public class LLProductionList {
	public HashMap<String, Production> tokenChoices = new HashMap<String, Production>();
	public HashMap<String, Production> quotedChoices = new HashMap<String, Production>();
	private Production handleEmpty;
	private final String name;
	
	public LLProductionList(String s, List<Production> lp) {
		this.name = s;
		for (Production p : lp)
		{
			if (p.isEmpty())
				handleEmpty = p;
			else if (p.first() instanceof LLGrammar.Quoted)
				quotedChoices.put(p.first().text(), p);
			else if (p.first() instanceof LLGrammar.Token)
				tokenChoices.put(p.first().text(), p);
		}
	}

	public boolean augment(List<Production> lp, Map<String, LLProductionList> productions) {
		boolean ret = false;
		for (Production p : lp)
		{
			for (ProductionElement e : p)
			{
				if (e instanceof LLGrammar.NonTerm)
				{
					if (!productions.containsKey(e.text()))
						throw new UtilException("There is no production for " + e.text());
				}
			}
			if (!p.isEmpty() && p.first() instanceof LLGrammar.NonTerm)
			{
				LLProductionList nt = productions.get(p.first().text());
				if (nt == null)
					throw new UtilException("Incomplete Grammar: no non-terminal " + p.first().text());
				for (String s : nt.quotedChoices.keySet())
				{
					// if we already have it, it better go to the same place
					if (quotedChoices.containsKey(s))
					{
						if (quotedChoices.get(s) != p)
							throw new UtilException("Conflict during augmentation of " + name + ".  Currently token " + s + " maps to " + quotedChoices.get(s) + "; cannot also map to " + p);
					}
					else
					{
						// capture this one, and record true
						quotedChoices.put(s, p);
						ret = true;
					}
				}
				for (String s : nt.tokenChoices.keySet())
				{
					// if we already have it, it better go to the same place
					if (tokenChoices.containsKey(s))
					{
						if (tokenChoices.get(s) != p)
							throw new UtilException("Conflict during augmentation of " + name + ".  Currently token " + s + " maps to " + tokenChoices.get(s) + "; cannot also map to " + p);
					}
					else
					{
						// capture this one, and record true
						tokenChoices.put(s, p);
						ret = true;
					}
				}
			}		
		}
		
		return ret;
	}

	public Production choose(LLToken next) {
		if (next == null)
		{
			if (handleEmpty == null)
				throw new UtilException("This rule cannot be empty");
			return handleEmpty;
		}
		String tag = next.tag();
		String text = next.text();
		// System.out.println(name +" has been asked to choose a rule based on: {" + tag +" => " + text + "}");
		if (quotedChoices.containsKey(text))
			return quotedChoices.get(text);
		if (tokenChoices.containsKey(tag))
			return tokenChoices.get(tag);
		if (handleEmpty != null)
			return handleEmpty;
		throw new UtilException("There is no matching rule at " + next);
	}

	public void prettyPrint(PrettyPrinter pp) {
		pp.requireNewline();
		pp.append("Quoted Choices: {");
		pp.indentMore();
		for (Entry<String, Production> qc : quotedChoices.entrySet())
		{
			pp.requireNewline();
			pp.append(qc.getKey() + " => " + qc.getValue());
			pp.requireNewline();
		}
		pp.indentLess();
		pp.append("}");
		pp.requireNewline();
		pp.append("Token Choices: {");
		pp.indentMore();
		for (Entry<String, Production> tc : tokenChoices.entrySet())
		{
			pp.requireNewline();
			pp.append(tc.getKey() + " => " + tc.getValue());
			pp.requireNewline();
		}
		pp.indentLess();
		pp.append("}");
	}

}
