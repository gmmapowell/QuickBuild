package com.gmmapowell.android;

import java.io.File;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.xml.XML;
import com.gmmapowell.xml.XMLElement;
import com.gmmapowell.xml.XMLNamespace;

public class ManifestCreator {
	private final XML out;
	private final XMLNamespace android;
	private final XMLElement top;
	private final XMLElement application;
	private String minSdkVersion = "4";
	private final String pkg;

	public ManifestCreator(String pkg) {
		this.pkg = pkg;
		out = XML.create("1.0", "manifest");
		android = out.namespace("android", "http://schemas.android.com/apk/res/android");
		top = out.top();
		top.setAttribute("package", pkg);
		top.setAttribute(android.attr("versionCode"), "1");
		top.setAttribute(android.attr("versionName"), "1.0");
		application = top.addElement("application"); // params?
	}
	
	public void save(File saveTo)
	{
		top.addElement("uses-sdk");
		top.setAttribute(android.attr("minSdkVersion"), minSdkVersion);
		out.write(saveTo);
	}

	public ManifestActivity addActivity(String id, String name, String label) {
		if (!id.startsWith(pkg + "."))
			throw new UtilException("The activity " + id + " does not start with " + pkg);
		return new ManifestActivity(android, application, id.substring(pkg.length()+1), name, label);
	}
}
