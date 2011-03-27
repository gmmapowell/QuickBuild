package com.gmmapowell.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PatternStore {
	private final Pattern pattern;
	private final String[] storeIn;
	private final String id;

	public PatternStore(String string, String id, String[] storeIn) {
		this.id = id;
		this.pattern = Pattern.compile(string);
		this.storeIn = storeIn;
	}

	public Matcher matcher(String s) {
		return pattern.matcher(s);
	}

	public void match(List<LinePatternMatch> ret, String s) {
		Matcher m = pattern.matcher(s);
		if (m.matches())
		{
			ret.add(new LinePatternMatch(m, id, storeIn));
		}
		
	}

}
