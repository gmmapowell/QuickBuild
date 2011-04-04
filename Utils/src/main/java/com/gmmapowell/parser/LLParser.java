package com.gmmapowell.parser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LLGrammar.Production;
import com.gmmapowell.parser.LLGrammar.ProductionElement;
import com.gmmapowell.parser.LLGrammar.TokenMatcher;

public class LLParser {
	class InputState {
		private final LineNumberReader lnr;
		private LLToken nextToken;
		private String currentInput;

		public InputState(Reader r) {
			lnr = new LineNumberReader(r);
		}

		public boolean eof() {
			return next() == null;
		}

		LLToken next() {
			if (nextToken != null)
				return nextToken;
			return (nextToken = tokenize());
		}

		private LLToken tokenize() {
			try {
				while (true)
				{
					if (currentInput == null)
					{
						if ((currentInput = lnr.readLine()) == null)
							return null;
					}
					// TODO: should handle indent-significant
					int i = 0;
					while (i < currentInput.length() && Character.isWhitespace(currentInput.charAt(i)))
						i++;
					if (i == currentInput.length())
					{
						currentInput = null;
					    if (grammar.newlinesSignificant())
					    	return LLToken.newline();
					    continue;
					}
					// it should match one or more of the patterns ...
					List<TokenMatcher> matchers = new ArrayList<TokenMatcher>();
					matchers.addAll(grammar.tokens);
					int j=i;
					TokenMatcher tm = null;
					while (j < currentInput.length() && !Character.isWhitespace(currentInput.charAt(j)))
						j++;
					while (j > i && (tm = match(matchers, currentInput.substring(i,j))) == null)
						j--;
					if (j == i)
						throw new RuntimeException("No tokens could match at: " + currentInput.substring(i));
					LLToken tok = new LLToken(tm, currentInput.substring(i,j));
					currentInput = currentInput.substring(j);
					return tok;
				}
			} catch (IOException e) {
				throw UtilException.wrap(e);
			}
		}

		private TokenMatcher match(List<TokenMatcher> matchers, String substring) {
			TokenMatcher ret = null;
//			System.out.println("Matching " + substring);
			for (TokenMatcher tm : matchers)
			{
				if (tm.matches(substring))
				{
//					System.out.println("** " + tm);
					if (ret == null)
						ret = tm;
				}
//				else
//					System.out.println(tm);
			}
			return ret;
		}

		public void advance() {
			if (nextToken == null)
				throw new RuntimeException("Cannot advance without examining token");
			nextToken = null;
		}
	}

	private final LLGrammar grammar;

	public LLParser(LLGrammar grammar)
	{
		this.grammar = grammar;
		
	}

	public LLTree parse(Reader r) {
		InputState is = new InputState(r);
		LLTree ret = parse(is, grammar.top());
		if (!is.eof())
			throw new RuntimeException("Did not reach end-of-file");
		return ret;
	}

	LLTree parse(InputState is, LLProductionList production) {
		Production p = production.choose(is.next());
//		System.out.println("Chose rule " + p);
		List<Object> nts = new ArrayList<Object>();
		for (ProductionElement pe : p)
		{
			Object o = pe.dealWith(this, is);
			if (o != null)
				nts.add(o);
		}
		return new LLTree(p, nts);
	}
}
