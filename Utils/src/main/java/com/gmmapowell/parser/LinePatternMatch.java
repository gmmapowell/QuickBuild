package com.gmmapowell.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import com.gmmapowell.exceptions.UtilException;

public class LinePatternMatch {
	private Map<String, String> contents = new HashMap<String, String>();
	private final String id;
	
	public LinePatternMatch(Matcher m, String id, String[] storeIn) {
		this.id = id;
		int cnt = 2; // offset from 2 'cos 0 is the whole thing and we add our own group for the match at 1
		for (String s : storeIn)
		{
			contents.put(s, m.group(cnt++));
		}
	}

	public boolean is(String string) {
		return id.equals(string);
	}

	public String get(String string) {
		if (!contents.containsKey(string))
			throw new UtilException("No element " + string + " was captured");
		return contents.get(string);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id = " + id);
		sb.append(" match = " + contents);
		return sb.toString();
	}

}
