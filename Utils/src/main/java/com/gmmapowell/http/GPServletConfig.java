package com.gmmapowell.http;

import java.io.File;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

public class GPServletConfig implements ServletConfig {

	private GPServletContext servletContext;
	private final InlineServer inlineServer;
	private final GPServletDefn servletDefn;

	public GPServletConfig(InlineServer inlineServer, GPServletDefn servletDefn) {
		this.inlineServer = inlineServer;
		this.servletDefn = servletDefn;
		servletContext = new GPServletContext(this);
	}

	public void initParam(String key, String value) {
		servletContext.initParam(key, value);
	}

	@Override
	public String getInitParameter(String s) {
		return servletContext.getInitParameter(s);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return servletContext.getInitParameterNames();
	}

	@Override
	public GPServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public String getServletName() {
		if (servletDefn != null)
			return servletDefn.getServletClass();
		return null;
	}

	public List<File> staticPaths(String s) {
		return inlineServer.staticPaths(s);
	}

	public List<String> resourceRoots() {
		return inlineServer.staticResources;
	}

	HttpServlet getServlet() {
		if (servletDefn != null)
			return servletDefn.getImpl();
		return null;
	}

}
