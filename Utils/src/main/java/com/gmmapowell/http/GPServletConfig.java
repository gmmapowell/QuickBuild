package com.gmmapowell.http;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class GPServletConfig implements ServletConfig {

	private GPServletContext servletContext;

	public GPServletConfig() {
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
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public String getServletName() {
		// TODO Auto-generated method stub
		return null;
	}

}
