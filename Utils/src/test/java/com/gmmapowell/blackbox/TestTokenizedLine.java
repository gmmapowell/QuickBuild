package com.gmmapowell.blackbox;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.zinutils.exceptions.UtilException;

import com.gmmapowell.parser.TokenizedLine;

public class TestTokenizedLine {
	@Test
	public void testBlankIsOK() {
		TokenizedLine toks = new TokenizedLine(1, "");
		assertEquals(0, toks.indent);
		assertEquals(0, toks.tokens.length);
	}

	@Test
	public void testCommentInColumn0() {
		TokenizedLine toks = new TokenizedLine(1, "# hello");
		assertEquals(0, toks.indent);
		assertEquals(0, toks.tokens.length);
	}

	@Test
	public void testCommentInColumn3Begin() {
		TokenizedLine toks = new TokenizedLine(1, "   # hello");
		assertEquals(3, toks.indent);
		assertEquals(0, toks.tokens.length);
	}

	@Test
	public void testTokenInColumn0() {
		TokenizedLine toks = new TokenizedLine(1, "hello");
		assertEquals(0, toks.indent);
		assertEquals(1, toks.tokens.length);
		assertEquals("hello", toks.tokens[0]);
	}

	@Test
	public void testTokenInColumn0WithTrailingWS() {
		TokenizedLine toks = new TokenizedLine(1, "hello  ");
		assertEquals(0, toks.indent);
		assertEquals(1, toks.tokens.length);
		assertEquals("hello", toks.tokens[0]);
	}

	@Test
	public void testTokenInColumn3WithTrailingWS() {
		TokenizedLine toks = new TokenizedLine(1, "   hello  ");
		assertEquals(3, toks.indent);
		assertEquals(1, toks.tokens.length);
		assertEquals("hello", toks.tokens[0]);
	}

	@Test
	public void testCommentAfter1stTokInColumn3() {
		TokenizedLine toks = new TokenizedLine(1, "   hello # world");
		assertEquals(3, toks.indent);
		assertEquals(1, toks.tokens.length);
		assertEquals("hello", toks.tokens[0]);
	}

	@Test
	public void testTwoTokenInColumn3() {
		TokenizedLine toks = new TokenizedLine(1, "   hello world ");
		assertEquals(3, toks.indent);
		assertEquals(2, toks.tokens.length);
		assertEquals("hello", toks.tokens[0]);
		assertEquals("world", toks.tokens[1]);
	}
	
	@Test
	public void testSingleQuotedStringWrapsSpace() {
		TokenizedLine toks = new TokenizedLine(1, "   'hello world'");
		assertEquals(3, toks.indent);
		assertEquals(1, toks.tokens.length);
		assertEquals("hello world", toks.tokens[0]);
	}
	
	@Test
	public void testSingleQuotedStringWrapsQUOT() {
		TokenizedLine toks = new TokenizedLine(1, "   'hello\"world'");
		assertEquals(3, toks.indent);
		assertEquals(1, toks.tokens.length);
		assertEquals("hello\"world", toks.tokens[0]);
	}
	
	@Test
	public void testSingleQuotedStringCanNestQuote() {
		TokenizedLine toks = new TokenizedLine(1, "   'hello''world'");
		assertEquals(3, toks.indent);
		assertEquals(1, toks.tokens.length);
		assertEquals("hello'world", toks.tokens[0]);
	}

	@Test
	public void testAQuotedStringCanBeFollowed() {
		TokenizedLine toks = new TokenizedLine(1, "   'hello' 'world'");
		assertEquals(3, toks.indent);
		assertEquals(2, toks.tokens.length);
		assertEquals("hello", toks.tokens[0]);
		assertEquals("world", toks.tokens[1]);
	}

	@Test(expected=UtilException.class)
	public void testStringCannotContinueAfterCloseQuote() {
		new TokenizedLine(1, "   'hello'world'");
	}
	
	@Test(expected=UtilException.class)
	public void testStringCannotHaveTabInIndent() {
		new TokenizedLine(1, "\t");
	}
}
