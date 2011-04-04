package com.gmmapowell.parser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.collections.ListMap;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LLParser.InputState;
import com.gmmapowell.utils.PrettyPrinter;

// It really does feel like we want to parse the grammar file by "recursively" invoking
// the parser on a well-known grammar
public class LLGrammar {
	public static interface ProductionElement {

		Object dealWith(LLParser parser, InputState is);

		String text();

	}

	public static class Quoted implements ProductionElement {

		private final String text;

		public Quoted(String text) {
			this.text = text;
		}

		@Override
		public Object dealWith(LLParser parser, InputState is) {
			if (is.next().text().equals(text))
			{
				is.advance();
				return null;
			}
			throw new UtilException("Expected " + text + " but found " + is.next().text());
		}

		@Override
		public String text() {
			return text;
		}

	}

	public static class Token implements ProductionElement {

		private final String text;

		public Token(String text) {
			this.text = text;
		}

		@Override
		public Object dealWith(LLParser parser, InputState is) {
			LLToken ret = is.next();
			is.advance();
			return ret;
		}

		@Override
		public String text() {
			return text;
		}

	}

	public class NonTerm implements ProductionElement {

		private final String text;

		public NonTerm(String text) {
			this.text = text;
		}

		@Override
		public Object dealWith(LLParser parser, InputState is) {
			return parser.parse(is, findProductionList(text));
		}

		@Override
		public String text() {
			return text;
		}

	}

	public static class TokenMatcher {
		private final String tag;
		private Pattern p;
		private final String text;

		public TokenMatcher(String tag, String pattern) {
			this.text = pattern;
			if (tag.equals("newline") || tag.equals("indent"))
				throw new RuntimeException(tag + " is a reserved token");
			this.tag = tag;
			p = Pattern.compile(pattern);
		}

		public boolean matches(String input) {
			return p.matcher(input).matches();
		}

		public String tag() {
			return tag;
		}
		
		public void prettyPrint(PrettyPrinter pp) {
			pp.requireNewline();
			pp.append("" + tag + " <= " + p);
		}
		
		@Override
		public String toString() {
			return "MM: " + tag + " << " + p;
		}

		public String text() {
			return text;
		}
	}
	
	public static class Production implements Iterable<ProductionElement> {
		private final String name;
		private final ProductionElement[] elts;

		public Production(String name, ProductionElement[] elts) {
			this.name = name;
			this.elts = elts;
		}

		public int length() {
			return elts.length;
		}

		@Override
		public Iterator<ProductionElement> iterator() {
			return CollectionUtils.listOf(elts).iterator();
		}

		public boolean isEmpty() {
			return elts.length == 0;
		}

		public ProductionElement first() {
			return elts[0];
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			sb.append(name);
			sb.append(":");
			for (ProductionElement pe : elts)
				sb.append(" " +pe.text());
			sb.append("}");
			return sb.toString();
		}

		public String result() {
			return name;
		}
	}
	
	private static final LLGrammar grammarGrammar = new LLGrammar();
	private final ListMap<String, Production> productions = new ListMap<String, Production>();
	private final Map<String, LLProductionList> productionList = new HashMap<String, LLProductionList>();
	final List<TokenMatcher> tokens = new ArrayList<LLGrammar.TokenMatcher>();
	private boolean significantNewlines = false; 
	private String top;

	private LLGrammar() {
	}
	
	public static LLGrammar read(Reader r)
	{
		LLGrammar ret = readNoComplete(r);
		ret.complete();
		return ret;
	}

	public static LLGrammar readNoComplete(Reader r)
	{
		LLGrammar ret = new LLGrammar();
		LLParser p = new LLParser(grammarGrammar);
		LLTree tree = p.parse(r);
//		System.out.println(tree);
		for (LLTree t : tree.items("Production"))
		{
			LLToken define = t.route("nonterm", 0, 0);
			ret.production(define.text(), getArgs(ret, t));
			for (LLTree c : t.items("Continuation"))
			{
				ret.production(define.text(), getArgs(ret, c));
			}
		}
		for (LLTree s : tree.items("Symbol"))
		{
			System.out.println(s);
			LLToken sym = s.route("token", 0);
			LLToken pattern = s.route("pattern", 1);
			ret.token(sym.text(), pattern.text());
		}
		
		// this is repeatable and doesn't throw errors
		ret.completePhase1();
		
//		grammarGrammar.production("Grammar", grammarGrammar.new NonTerm("ProductionList"), grammarGrammar.new NonTerm("SymbolList"));
//		grammarGrammar.production("ProductionList", grammarGrammar.new NonTerm("Production"), grammarGrammar.new NonTerm("OptionalProductionList"));
		return ret;
	}

	private static ProductionElement[] getArgs(LLGrammar ret, LLTree t) {
		List<ProductionElement> args = new ArrayList<ProductionElement>();
		for (LLTree i : t.items("Item", 0))
			args.add(ret.mapTokenToElement(i.route(null, 0)));
		ProductionElement[] linargs = args.toArray(new ProductionElement[args.size()]);
		return linargs;
	}

	private ProductionElement mapTokenToElement(LLToken token) {
		if (token.tag().equals("nonterm"))
			return new NonTerm(token.text());
		else if (token.tag().equals("quoted"))
			return new Quoted(token.text());
		else if (token.tag().equals("token"))
			return new Token(token.text());
		else
			throw new UtilException("Cannot handle item with token of type " + token.tag());
	}

	public LLProductionList top() {
		if (top == null)
			throw new RuntimeException("Must declare at least one production");
		if (productionList.isEmpty())
			complete();
		return findProductionList(top);
	}

	private LLProductionList findProductionList(String name) {
		LLProductionList ret = productionList.get(name);
		if (ret == null)
			throw new UtilException("There is no production list " + name);
		return ret;
	}

	private void complete() {
		completePhase1();
		
		// now loop through all the productions, sucking up the initial terms of all initial nonterms
		while (true)
		{
			boolean loop = false;
			for (String s : productionList.keySet())
			{
				LLProductionList ll = productionList.get(s);
				loop |= ll.augment(productions.get(s), productionList);
			}
			if (!loop)
				break;
		}
		
		Set<String> missing = new TreeSet<String>();
		loop:
		for (LLProductionList ll : productionList.values())
		{
			for (String tok : ll.tokenChoices.keySet())
			{
				for (TokenMatcher lt : tokens)
					if (lt.tag.equals(tok))
						continue loop;
				missing.add(tok);
			}
		}
		if (missing.size() > 0)
		{
			StringBuilder sb = new StringBuilder("The following tokens were not defined:");
			for (String s: missing)
				sb.append(" " + s);
			System.out.println(this);
			throw new UtilException(sb.toString());
		}
//		System.out.println(this);
	}

	private void completePhase1() {
		// first build up the basic list of productions, and find terminals that apply
		for (String s : productions)
		{
			List<Production> lp = productions.get(s);
			LLProductionList ll = new LLProductionList(s, lp);
			productionList.put(s, ll);
		}
	}

	@Override
	public String toString() {
		return prettyPrint();
	}
	
	private String prettyPrint() {
		PrettyPrinter pp = new PrettyPrinter();
		pp.append("Tokens:");
		pp.requireNewline();
		pp.indentMore();
		for (TokenMatcher s : tokens)
		{
			s.prettyPrint(pp);
		}
		pp.indentLess();
		for (Entry<String, LLProductionList> entry : productionList.entrySet())
		{
			pp.requireNewline();
			pp.append(entry.getKey() + ":");
			pp.indentMore();
			pp.requireNewline();
			entry.getValue().prettyPrint(pp);
			pp.indentLess();
		}
		return pp.toString();
	}

	private void production(String string, ProductionElement... elts) {
		if (!productionList.isEmpty())
			throw new UtilException("Cannot add more productions when complete");
		for (ProductionElement e : elts)
		{
			if (e instanceof Quoted)
			{
				String mapped = mapQuotedChars(e.text());
				boolean matched = false;
				for (TokenMatcher t : tokens)
				{
					if (t.text().equals(mapped))
					{
						matched = true;
						break;
					}
				}
				if (!matched)
					tokens.add(new TokenMatcher("quoted", mapped));
			}
			else if (e instanceof Token)
			{
				if (e.text().equals("newline"))
					significantNewlines = true;
			}
		}
		productions.add(string, new Production(string, elts));
		if (top == null)
			top = string;
	}

	private String mapQuotedChars(String text) {
		StringBuilder sb = new StringBuilder(text);
		for (int i=0;i<sb.length();i++)
			if (sb.charAt(i) == '|' || sb.charAt(i) == '(' || sb.charAt(i) == ')')
				sb.insert(i++, '\\');
		return sb.toString();
	}

	private void token(String tag, String pattern) {
		if (!productionList.isEmpty())
			throw new UtilException("Cannot add more token definitions when complete");
		tokens.add(new TokenMatcher(tag, pattern));
	}
	
	public boolean newlinesSignificant() {
		return significantNewlines;
	}

	static {
		/*
		Grammar          = ProductionList SymbolList
		ProductionList   = Production OptionalProductionList
		OptionalProductionList = "|" ProductionList
		                       |
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
		SymbolList       =
		                 | symbol "=" pattern
		                 
		nonterm = [A-Z][a-zA-z]*
		token = [a-z][a-zA-z]*
		quoted = "[^"]+"
		pattern = [a-zA-Z0-9.\[\]\\\"\']+
		 */
		grammarGrammar.production("Grammar", grammarGrammar.new NonTerm("ProductionList"), grammarGrammar.new NonTerm("SymbolList"));
		grammarGrammar.production("ProductionList", grammarGrammar.new NonTerm("Production"), grammarGrammar.new NonTerm("OptionalProductionList"));
		grammarGrammar.production("OptionalProductionList");
		grammarGrammar.production("OptionalProductionList", grammarGrammar.new NonTerm("ProductionList"));
		grammarGrammar.production("Production", grammarGrammar.new NonTerm("Define"), grammarGrammar.new NonTerm("ContinuationList"));
		grammarGrammar.production("ContinuationList");
		grammarGrammar.production("ContinuationList", grammarGrammar.new NonTerm("Continuation"), grammarGrammar.new NonTerm("ContinuationList"));
		grammarGrammar.production("Define", new Token("nonterm"), new Quoted("="), grammarGrammar.new NonTerm("ItemList"), new Token("newline"));
		grammarGrammar.production("Continuation", new Quoted("|"), grammarGrammar.new NonTerm("ItemList"), new Token("newline"));
		grammarGrammar.production("ItemList");
		grammarGrammar.production("ItemList", grammarGrammar.new NonTerm("Item"), grammarGrammar.new NonTerm("ItemList"));
		grammarGrammar.production("Item", new Token("nonterm"));
		grammarGrammar.production("Item", new Token("token"));
		grammarGrammar.production("Item", new Token("quoted"));
		grammarGrammar.production("SymbolList");
		grammarGrammar.production("SymbolList", grammarGrammar.new NonTerm("Symbol"), grammarGrammar.new NonTerm("SymbolList"));
		grammarGrammar.production("Symbol", new Token("token"), new Quoted("~"), new Token("pattern"), new Token("newline"));
		
		
		grammarGrammar.token("nonterm", "[A-Z][a-zA-z]*");
		grammarGrammar.token("token", "[a-z][a-zA-z]*");
		grammarGrammar.token("quoted", "\"[^\"]+\"");
		grammarGrammar.token("pattern", "[a-zA-Z0-9.(){}\\[\\]\\\\\"'\\-+*/=?!@#$%^&]+");
	}
}
