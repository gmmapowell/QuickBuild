package com.gmmapowell.http;

import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class GPServletDefn {
	private final String servletClass;
	private HttpServlet servletImpl;
	protected final GPServletConfig config;
	private String contextPath;
	private String servletPath;

	public GPServletDefn(InlineServer server, String servletClass) {
		config = new GPServletConfig(server, this);
		this.servletClass = servletClass;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
		((GPServletContext)config.getServletContext()).setContextPath(contextPath);
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
		((GPServletContext)config.getServletContext()).setServletPath(servletPath);
	}

	public void init() throws ClassNotFoundException, InstantiationException, IllegalAccessException, ServletException {
		Class<?> forName = Class.forName(servletClass);
		servletImpl = (HttpServlet) forName.newInstance();
		servletImpl.init(config);
	}

	public HttpServlet getImpl() {
		return servletImpl;
	}

	public void initParam(String key, String value) {
		config.initParam(key, value);
	}

	public boolean isForMe(String rawUri) {
		return rawUri.startsWith(contextPath+servletPath);
	}

	public GPServletConfig getConfig() {
		return config;
	}
}