package com.gmmapowell.blackbox;

import java.io.StringReader;

import org.junit.Test;

import com.gmmapowell.parser.LLGrammar;


public class TestLLParser {

	@Test
	public void testParser() throws Exception {
		LLGrammar.read(new StringReader("token Nonterm \"new\""));
	}
}
