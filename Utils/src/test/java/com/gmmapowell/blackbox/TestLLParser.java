package com.gmmapowell.blackbox;

import java.io.StringReader;

import org.junit.Test;

import com.gmmapowell.parser.LLGrammar;


public class TestLLParser {

	@Test
	public void testFirstLine() throws Exception {
		LLGrammar.read(new StringReader("Expr = OpExpr\n"));
	}

	@Test
	public void testSecondLine() throws Exception {
		LLGrammar.read(new StringReader("Expr = OpExpr\n   | \"new\" symbol\n"));
	}
}
