package com.gmmapowell.utils;

import java.util.List;

public class StringUtil {

	public static String concat(List<String> errors) {
		StringBuilder sb = new StringBuilder();
		for (String s : errors)
		{
			sb.append(s);
			sb.append("\n");
		}
		return sb.toString();
	}

	// TODO: this needs a proper implementation
	public static boolean globMatch(String pattern, String string) {
		if (pattern.startsWith("*"))
			return string.endsWith(pattern.substring(1));
		else
			return string.equals(pattern);
	}

}
