package com.gmmapowell.blackbox;

import java.io.StringReader;

import org.junit.Test;

import org.zinutils.parser.LLGrammar;


public class TestLLParser {

	public void testFirstLine() throws Exception {
		LLGrammar parsed = LLGrammar.readNoComplete(new StringReader("Expr = OpExpr\n"));
		System.out.println(parsed);
	}

	@Test
	public void testSecondLine() throws Exception {
		LLGrammar parsed = LLGrammar.readNoComplete(new StringReader("Expr = OpExpr\n   | \"new\" symbol\n"));
		System.out.println(parsed);
	}
	
	@Test
	public void testTwoDefinitions() throws Exception {
		LLGrammar parsed = LLGrammar.readNoComplete(new StringReader("Expr = OpExpr\n   | \"new\" symbol\nOpExpr = NestedExpr\n"));
		System.out.println(parsed);
	}
	
	@Test
	public void testSymbolDefn() throws Exception {
		LLGrammar parsed = LLGrammar.readNoComplete(new StringReader("Expr = OpExpr\nnumber ~ -?[0-9]+\n"));
		System.out.println(parsed);
	}
	}
