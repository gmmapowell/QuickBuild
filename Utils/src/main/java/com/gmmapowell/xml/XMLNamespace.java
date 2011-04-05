package com.gmmapowell.xml;

public class XMLNamespace {
	private String qualifier;
	private String uri;

	public XMLNamespace(String ns, String url) {
		qualifier = ns;
		uri = url;
	}

	public XMLNSAttr attr(String field) {
		return new XMLNSAttr(uri, qualifier + ":" + field);
	}

}
