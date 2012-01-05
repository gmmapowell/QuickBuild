package com.gmmapowell.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.ProcessArgs;

public class TokenizedLine {
	public static enum Mode {
		BEGIN, SPACE, TEXT, SINGLE_QUOTE, DOUBLE_QUOTE, ANGLE_QUOTE;

		public boolean isSpace() {
			return this == BEGIN || this == SPACE;
		}

		public boolean isText() {
			return this == TEXT || this == SINGLE_QUOTE || this == DOUBLE_QUOTE || this == ANGLE_QUOTE;
		}

		public boolean isQuoted() {
			return this == SINGLE_QUOTE || this == DOUBLE_QUOTE || this == ANGLE_QUOTE;
		}

		public boolean isIndent() {
			return this == BEGIN;
		}
	}

	public final int lineNo;
	public final int indent;
	public final String[] tokens;
	
	public TokenizedLine(int lineNo, String str) {
		this.lineNo = lineNo;
		Mode mode = Mode.BEGIN;
		int tokStart = -1;
		boolean pendingQuote = false;
		List<String> toks = new ArrayList<String>();
		int ind = 0;
		StringBuilder s = new StringBuilder(str);
		for (int pos = 0; pos < s.length(); pos++)
		{
			char c = s.charAt(pos);
			if (mode.isIndent())
				ind = pos;
			if (Character.isWhitespace(c))
			{
				if (mode.isIndent() && c == '\t')
					throw new UtilException("Tabs are not permitted in the indent");
				if (mode.isSpace())
					continue;
				else if (pendingQuote)
					toks.add(s.substring(tokStart+1, pos-1));
				else if (mode.isQuoted())
					continue;
				else
					toks.add(s.substring(tokStart, pos));
				mode = Mode.SPACE;
			}
			else
			{
				if ((mode == Mode.SINGLE_QUOTE && c == '\'') || (mode == Mode.DOUBLE_QUOTE && c == '"') || (mode == Mode.ANGLE_QUOTE && c == '>'))
				{
					if (pendingQuote)
					{
						s.delete(pos, pos+1);
						pos--;
						pendingQuote = false;
						continue;
					}
					pendingQuote = true;
				}
				else if (pendingQuote)
					throw new UtilException("Cannot continue a string after the end quote");
				if (!mode.isQuoted() && c == '#')
					break;
				if (mode.isText())
					continue;
				else if (mode.isSpace())
				{
					if (c == '\'')
						mode = Mode.SINGLE_QUOTE;
					else if (c == '"')
						mode = Mode.DOUBLE_QUOTE;
					else if (c == '<')
						mode = Mode.ANGLE_QUOTE;
					else
						mode = Mode.TEXT;
					tokStart = pos;
				}
			}
		}
		if (pendingQuote)
			toks.add(s.substring(tokStart+1, s.length()-1));
		else if (mode.isQuoted())
			throw new UtilException("Cannot end line in the middle of a string");
		else if (mode.isText())
			toks.add(s.substring(tokStart));
		indent = ind;
		tokens = toks.toArray(new String[toks.size()]);
	}

	public int length() {
		return tokens.length;
	}

	public boolean blank() {
		return tokens.length == 0;
	}

	public String cmd() {
		if (blank())
			throw new UtilException("You cannot ask for the command from a blank string");
		return tokens[0];
	}

	public <T> void process(T into, ArgumentDefinition... defns) {
		String[] args = Arrays.copyOfRange(tokens, 1, length());
		ProcessArgs.process(into, defns, args);
	}

	public int lineNo() {
		return lineNo;
	}

	public int indent() {
		return indent;
	}
	
	@Override
	public String toString() {
		if (tokens == null)
			return "<null>";
		ArrayList<String> ret = new ArrayList<String>();
		for (String s : tokens)
			ret.add(s);
		return ret.toString();
	}

}
