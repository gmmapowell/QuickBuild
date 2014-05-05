package com.gmmapowell.http;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.gmmapowell.exceptions.UtilException;

@SuppressWarnings("deprecation")
public class GPHttpSession implements HttpSession {
	final String cookie;
	private Map<String,Object> attributes = new HashMap<String, Object>();
	private final GPServletContext context;
	private int maxInactive;
	private long lastAccess;
	private boolean isNew = true;

	public GPHttpSession(GPServletContext context, String cookie) {
		this.context = context;
		this.cookie = cookie;
		this.lastAccess = new Date().getTime();
	}

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new UtilException("Not Implemented");
	}

	@Override
	public long getCreationTime() {
		throw new UtilException("Not Implemented");
	}

	@Override
	public String getId() {
		throw new UtilException("Not Implemented");
	}

	@Override
	public long getLastAccessedTime() {
		return lastAccess;
	}

	@Override
	public int getMaxInactiveInterval() {
		return maxInactive;
	}

	@Override
	public ServletContext getServletContext() {
		throw new UtilException("Not Implemented");
	}

	@Override
	public HttpSessionContext getSessionContext() {
		throw new UtilException("Not Implemented");
	}

	@Override
	public Object getValue(String arg0) {
		throw new UtilException("Not Implemented");
	}

	@Override
	public String[] getValueNames() {
		throw new UtilException("Not Implemented");
	}

	@Override
	public void invalidate() {
		context.deleteSession(this);
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@Override
	public void putValue(String arg0, Object arg1) {
		throw new UtilException("Not Implemented");
	}

	@Override
	public void removeAttribute(String arg0) {
		throw new UtilException("Not Implemented");
	}

	@Override
	public void removeValue(String arg0) {
		throw new UtilException("Not Implemented");
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		attributes.put(arg0, arg1);
	}

	@Override
	public void setMaxInactiveInterval(int arg0) {
		maxInactive = arg0;
	}

	public void accessed() {
		isNew = false;
		lastAccess = new Date().getTime();
	}
}
