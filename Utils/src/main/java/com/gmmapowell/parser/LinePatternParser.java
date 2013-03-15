package com.gmmapowell.parser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.utils.IterableReader;

public class LinePatternParser {
	private List<PatternStore> patterns = new ArrayList<PatternStore>();
	
	public void match(String oattern, String id, String... storeIn) {
		patterns.add(new PatternStore("("+oattern+")", false, id, storeIn));
	}

	public void matchAll(String pattern, String id, String... storeIn) {
		patterns.add(new PatternStore("("+pattern+")", true, id, storeIn));
	}
	
	public List<LinePatternMatch> applyTo(Reader reader) {
		IterableReader lnr = new IterableReader(reader);
		List<LinePatternMatch> ret = new ArrayList<LinePatternMatch>();
		for (String s : lnr)
		{
			for (PatternStore p : patterns)
			{
				p.match(ret, s);
			}
		}
		return ret;
	}

}
