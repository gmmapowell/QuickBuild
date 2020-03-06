package com.gmmapowell.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.ProcessArgs;

public class TokenizedLine {
	public static enum Mode {
		SPACE, TEXT, SINGLE_QUOTE, DOUBLE_QUOTE, ANGLE_QUOTE;

		public boolean isSpace() {
			return this == SPACE;
		}

		public boolean isText() {
			return this == TEXT || this == SINGLE_QUOTE || this == DOUBLE_QUOTE || this == ANGLE_QUOTE;
		}

		public boolean isQuoted() {
			return this == SINGLE_QUOTE || this == DOUBLE_QUOTE || this == ANGLE_QUOTE;
		}
	}

	public final int lineNo;
	public final int indent;
	public final String text;
	public final String[] tokens;
	
	public TokenizedLine(int lineNo, String str) {
		this(lineNo, str, true);
	}

	public TokenizedLine(int lineNo, String str, boolean tokenize) {
		this.lineNo = lineNo;
		int ind = 0;
		while (ind< str.length() && Character.isWhitespace(str.charAt(ind))) {
			if (str.charAt(ind) == '\t')
				throw new UtilException("Tabs are not permitted in the indent");
			ind++;
		}
		this.indent = ind;
		text = str.substring(ind);
		if (tokenize)
			this.tokens = tokenizeLine();
		else
			this.tokens = null;
	}

	private String[] tokenizeLine() {
		Mode mode = Mode.SPACE;
		int tokStart = -1;
		boolean pendingQuote = false;
		List<String> toks = new ArrayList<String>();
		StringBuilder s = new StringBuilder(text);
		for (int pos = 0; pos < s.length(); pos++)
		{
			char c = s.charAt(pos);
			if (Character.isWhitespace(c))
			{
				if (mode.isSpace())
					continue;
				else if (pendingQuote)
					toks.add(s.substring(tokStart+1, pos-1));
				else if (mode.isQuoted())
					continue;
				else
					toks.add(s.substring(tokStart, pos));
				mode = Mode.SPACE;
				pendingQuote = false;
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
		return toks.toArray(new String[toks.size()]);
	}

	public int length() {
		return tokens.length;
	}

	public boolean blank() {
		return text.length() == 0 || (tokens != null && tokens.length == 0);
	}

	public String cmd() {
		if (blank())
			throw new UtilException("You cannot ask for the command from a blank string");
		if (tokens != null && tokens.length > 0)
			return tokens[0];
		else
			return null;
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
			return "[ind:"+indent+"]:'" + text + "'";
		ArrayList<String> ret = new ArrayList<String>();
		for (String s : tokens)
			ret.add(s);
		return "[ind:"+indent+"]:" + ret.toString();
	}

}
