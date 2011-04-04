package com.gmmapowell.parser;

import com.gmmapowell.parser.LLGrammar.TokenMatcher;

public class LLToken {
	private final String tag;
	private final String text;

	public LLToken(TokenMatcher tm, String substring) {
		this(tm.tag(), substring);
	}

	public LLToken(String tag, String substring) {
		this.tag = tag;
		this.text = substring;
	}

	public static LLToken newline() {
		return new LLToken("newline", "\n");
	}
	
	@Override
	public String toString() {
		return "Token {" + tag + ": " + text + "}";
	}

	public String text() {
		return text;
	}

	public String tag() {
		return tag;
	}

}
