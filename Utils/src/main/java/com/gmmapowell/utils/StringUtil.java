package com.gmmapowell.utils;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;

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
	  
	public static String hex(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++)
			sb.append(hex(b[i] & 0xff, 2));
		return sb.toString().toUpperCase();
	}
	
	public static String capitalize(String s)
	{
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	public static String decapitalize(String s) {
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	public static Iterable<String> lines(String stderr) {
		try
		{
			List<String> ret = new ArrayList<String>();
			LineNumberReader lnr = new LineNumberReader(new StringReader(stderr));
			String s;
			while ((s = lnr.readLine()) != null)
				ret.add(s);
			return ret;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static byte[] fromHex(String s, int len) {
		if (len != 0 && s.length() != 2*len)
			throw new UtilException("Invalid string length: must be " + 2*len + " not " + s.length());
		byte[] ret = new byte[len];
		for (int i=0;i<len;i++)
			ret[i] = (byte) Integer.parseInt(s.substring(2*i,2*i+2), 16);
		return ret;
	}
}
