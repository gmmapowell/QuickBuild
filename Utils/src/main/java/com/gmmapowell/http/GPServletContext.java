package com.gmmapowell.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import com.gmmapowell.collections.IteratorEnumerator;
import com.gmmapowell.utils.FileUtils;

public class GPServletContext implements ServletContext {
	private Map<String, String> initParams = new HashMap<String, String>();
	private String contextPath;
	String servletPath;
	private Random rand = new Random();
	private Map<String, GPHttpSession> sessions = new HashMap<String, GPHttpSession>();
	
	public GPServletContext(GPServletConfig config) {
	}

	public void initParam(String key, String value) {
		initParams.put(key, value);
	}
	
	public void setContextPath(String path)
	{
		contextPath = path;
	}
	
	public void setServletPath(String path)
	{
		servletPath = path;
	}

	@Override
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getContext(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContextPath() {
		return contextPath;
	}

	@Override
	public String getInitParameter(String s) {
		return initParams.get(s);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return new IteratorEnumerator<String>(initParams.keySet().iterator());
	}

	@Override
	public int getMajorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getMimeType(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMinorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRealPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getResource(String arg0) throws MalformedURLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String s) {
		File f = new File(FileUtils.getCurrentDir().getAbsoluteFile(), s);
		if (f.exists())
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		return this.getClass().getResourceAsStream(s);
	}

	@Override
	public Set<String> getResourcePaths(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServerInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Servlet getServlet(String arg0) throws ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServletContextName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getServletNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getServlets() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void log(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void log(Exception arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void log(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	public GPHttpSession newSession() {
		String cookie = Long.toHexString(rand.nextLong());
		GPHttpSession s = new GPHttpSession(this, cookie);
		sessions.put(cookie, s);
		return s;
	}

	public void deleteSession(GPHttpSession gpHttpSession) {
		
	}
}
