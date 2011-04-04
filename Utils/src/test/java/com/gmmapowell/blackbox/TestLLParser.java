package com.gmmapowell.blackbox;

import java.io.StringReader;

import org.junit.Ignore;
import org.junit.Test;

import com.gmmapowell.parser.LLGrammar;


public class TestLLParser {

	@Test
	@Ignore
	public void testFirstLine() throws Exception {
		LLGrammar parsed = LLGrammar.readNoComplete(new StringReader("Expr = OpExpr\n"));
		System.out.println(parsed);
	}

	@Test
	public void testSecondLine() throws Exception {
		LLGrammar parsed = LLGrammar.readNoComplete(new StringReader("Expr = OpExpr\n   | \"new\" symbol\n"));
		System.out.println(parsed);
	}
}
