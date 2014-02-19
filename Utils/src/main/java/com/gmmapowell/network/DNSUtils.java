package com.gmmapowell.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import com.gmmapowell.exceptions.UtilException;

public class DNSUtils {

	public static List<MXRecord> getMXHosts(String domain) {
		List<MXRecord> ret = new ArrayList<MXRecord>();
		for (String s : getDNSEntriesFor(domain, "MX")) {
			String[] v = s.split("\\s+");
			ret.add(new MXRecord(Integer.parseInt(v[0]), v[1]));
		}
		Collections.sort(ret);
		return ret;
	}

	public static List<String> getTextRecords(String domain) {
		return getDNSEntriesFor(domain, "TXT");
	}

	public static Map<String, String> getTextMap(String domain) {
		List<String> text = getDNSEntriesFor(domain, "TXT");
		Map<String, String> ret = new TreeMap<String, String>();
		for (String s : text) {
			if (s.startsWith("\"") && s.endsWith("\""))
				s = s.substring(1, s.length()-1);
			int idx = s.indexOf('=');
			if (idx == -1)
				ret.put(s, "");
			else
				ret.put(s.substring(0, idx), s.substring(idx+1));
		}
		return ret;
	}

	private static List<String> getDNSEntriesFor(String domainName, String type)
	{
		try {
			InitialDirContext iDirC = new InitialDirContext();
			Attributes attributes = iDirC.getAttributes("dns:/" + domainName, new String[] { type });
			Attribute attribute = attributes.get(type);
			List<String> ret = new ArrayList<String>();
			for (int i=0;i<attribute.size();i++) {
				ret.add((String)attribute.get(i));
			}
			return ret;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
}
