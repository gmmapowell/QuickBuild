package com.gmmapowell.utils;

import java.util.List;

public class StringUtil {

	public static String concatVertically(List<String> errors) {
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
		else if (pattern.endsWith("*"))
			return string.startsWith(pattern.substring(0, pattern.length()-1));
		else
			return string.equals(pattern);
	}

	public static String concat(String...  args) {
		StringBuilder sb = new StringBuilder();
		for (String s : args)
			sb.append(s);
		return sb.toString();
	}

	public static String digits(int quant, int nd) {
		StringBuilder sb = new StringBuilder();
		sb.append(quant);
		if (sb.length() > nd)
			sb.delete(0, sb.length()-nd);
		while (sb.length() < nd)
			sb.insert(0, "0");
		return sb.toString();
	}

	public static String hex(int quant, int nd) {
		StringBuilder sb = new StringBuilder();
		sb.append(Integer.toHexString(quant).toUpperCase());
		if (sb.length() > nd)
			sb.delete(0, sb.length()-nd);
		while (sb.length() < nd)
			sb.insert(0, "0");
		return sb.toString();
	}
}
