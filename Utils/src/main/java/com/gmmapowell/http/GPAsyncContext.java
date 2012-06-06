package com.gmmapowell.http;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.gmmapowell.exceptions.UtilException;

public class GPAsyncContext implements AsyncContext {

	private final ServletRequest request;
	private final ServletResponse response;
	private final List<GPAsyncListener> listeners = new ArrayList<GPAsyncListener>();
	private long timeout;

	public GPAsyncContext(ServletRequest arg0, ServletResponse arg1) {
		this.request = arg0;
		this.response = arg1;
	}

	@Override
	public void addListener(AsyncListener arg0) {
		listeners.add(new GPAsyncListener(arg0, request, response));
	}

	@Override
	public void addListener(AsyncListener arg0, ServletRequest arg1,
			ServletResponse arg2) {
		throw new UtilException("Not implemented");
	}

	@Override
	public void complete() {
		throw new UtilException("Not implemented");
	}

	@Override
	public <T extends AsyncListener> T createListener(Class<T> arg0)
			throws ServletException {
		throw new UtilException("Not implemented");
	}

	@Override
	public void dispatch() {
		throw new UtilException("Not implemented");
	}

	@Override
	public void dispatch(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public void dispatch(ServletContext arg0, String arg1) {
		throw new UtilException("Not implemented");
	}

	@Override
	public ServletRequest getRequest() {
		return request;
	}

	@Override
	public ServletResponse getResponse() {
		return response;
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

	@Override
	public boolean hasOriginalRequestAndResponse() {
		throw new UtilException("Not implemented");
	}

	@Override
	public void setTimeout(long arg0) {
		timeout = arg0;
	}

	@Override
	public void start(Runnable arg0) {
		throw new UtilException("Not implemented");
	}

}
