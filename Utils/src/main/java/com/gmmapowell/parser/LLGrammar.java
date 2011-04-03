package com.gmmapowell.parser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// It really does feel like we want to parse the grammar file by "recursively" invoking
// the parser on a well-known grammar
public class LLGrammar {
	public static class TokenMatcher {
		private final String tag;
		private Pattern p;

		public TokenMatcher(String tag, String pattern) {
			this.tag = tag;
			p = Pattern.compile(pattern);
		}

		public boolean matches(String input) {
			return p.matcher(input).matches();
		}

		public String tag() {
			return tag;
		}
		
		@Override
		public String toString() {
			return "MM: " + tag + " << " + p;
		}
	}
	
	public static class Production {

	}
	
	List<TokenMatcher> tokens = new ArrayList<LLGrammar.TokenMatcher>();

	private LLGrammar() {
	}
	
	public static LLGrammar read(Reader r)
	{
		LLGrammar ret = new LLGrammar();
		LLParser p = new LLParser(grammarGrammar);
		LLTree tree = p.parse(r);
		return ret;
	}

	public Production top() {
		// TODO Auto-generated method stub
		return null;
	}

	private void token(String tag, String pattern) {
		tokens.add(new TokenMatcher(tag, pattern));
	}

	private static LLGrammar grammarGrammar = new LLGrammar();
	static {
		/*
		Grammar          = ProductionList SymbolList
		ProductionList   = Production
		                 | Production ProductionList
		Production       = Define ContinuationList
		ContinuationList =
		                 | Continuation ContinuationList
		Define           = nonterm "=" ItemList newline
		Continuation     = "|" ItemList newline
		ItemList         = 
		                 | Item ItemList
		Item             = nonterm
		                 | token
		                 | quoted
		                 
		nonterm = [A-Z][a-zA-z]*
		token = [a-z][a-zA-z]*
		quoted = "[^"]+"
		 */
		grammarGrammar.token("nonterm", "[A-Z][a-zA-z]*");
		grammarGrammar.token("token", "[a-z][a-zA-z]*");
		grammarGrammar.token("quoted", "\"[^\"]+\"");
//		grammarGrammar.token("quoted", "\"new\"");
	}
	
	public boolean newlinesSignificant() {
		// TODO Auto-generated method stub
		return false;
	}
}
