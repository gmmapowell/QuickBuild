package com.gmmapowell.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.gmmapowell.collections.IteratorEnumerator;
import com.gmmapowell.collections.ListMap;
import com.gmmapowell.exceptions.UtilException;

public class GPRequest implements HttpServletRequest {

	private static final Logger logger = Logger.getLogger("InlineServer");
	private final String method;
	private final URI uri;
	private final ListMap<String, String> headers = new ListMap<String, String>();
	private final GPServletContext context;
	private GPResponse response;
	private final String rawUri;
	private final InputStream is;
	private GPServletInputStream servletInputStream;
	private GPHttpSession session;
	private final List<Cookie> cookies = new ArrayList<Cookie>();
	private Cookie[] cookieArr;

	public GPRequest(GPServletContext context, String s, InputStream is) throws URISyntaxException {
		this.context = context;
		this.is = is;
		String[] command = s.split(" ");
		method = command[0];
		rawUri = command[1];
		uri = new URI(rawUri);
		logger.fine("Received " + method + " request for " + rawUri);
	}

	public void addHeader(String s) {
		int colon = s.indexOf(":");
		if (colon == -1)
			return;
		String hdr = s.substring(0, colon).toLowerCase();
		String value = s.substring(colon+1).trim();
		headers.add(hdr, value);
		if (hdr.equals("cookie"))
		{
			for (String c : value.split(";")) {
				String[] c1 = c.trim().split("=");
				Cookie cookie = new Cookie(c1[0], c1[1]);
				cookies.add(cookie);
				if (cookie.getName().equals("JSESSIONID"))
					session = context.getSession(cookie.getValue());
			}
		}
	}
	
	public void endHeaders() {
		// getIntHeader is a little fragile for something so important ...
		servletInputStream = new GPServletInputStream(is, getIntHeader("Content-Length"));
	}

	@Override
	public Object getAttribute(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getCharacterEncoding() {
		throw new UtilException("Not implemented");
	}

	@Override
	public int getContentLength() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getContentType() {
		throw new UtilException("Not implemented");
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return servletInputStream;
	}

	@Override
	public String getLocalAddr() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getLocalName() {
		throw new UtilException("Not implemented");
	}

	@Override
	public int getLocalPort() {
		throw new UtilException("Not implemented");
	}

	@Override
	public Locale getLocale() {
		throw new UtilException("Not implemented");
	}

	@Override
	public Enumeration<String> getLocales() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getParameter(String arg0) {
		String q = uri.getQuery();
		int k = q.indexOf(arg0 + "=");
		if (k == 0 || (k > 0 && q.charAt(k-1) == '&'))
		{
			int from = k+arg0.length()+1;
			int t = q.indexOf('&', from);
			if (t == -1)
				return q.substring(from);
			else
				return q.substring(from, t);
		}
		return null;
	}

	@Override
	public Map<String, String> getParameterMap() {
		// TODO: do we need to decode the %xx notation?
		Map<String,String> ret = new HashMap<String, String>();
		String q = uri.getQuery();
		int k=0;
		while (k < q.length())
		{
			int p = k;
			while (k < q.length() && q.charAt(k) != '=' && q.charAt(k) != '&')
				k++;
			String s = q.substring(p, k);
			if (k == q.length() || q.charAt(k) == '&')
			{
				ret.put(s, "");
				continue;
			}
			int v = ++k; // skip the =
			while (k < q.length() && q.charAt(k) != '&')
				k++;
			ret.put(s, q.substring(v, k));
			k++; // skip the &
		}
		return ret;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String[] getParameterValues(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getProtocol() {
		throw new UtilException("Not implemented");
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getRealPath(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getRemoteAddr() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getRemoteHost() {
		throw new UtilException("Not implemented");
	}

	@Override
	public int getRemotePort() {
		throw new UtilException("Not implemented");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getScheme() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getServerName() {
		throw new UtilException("Not implemented");
	}

	@Override
	public int getServerPort() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isSecure() {
		throw new UtilException("Not implemented");
	}

	@Override
	public void removeAttribute(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		throw new UtilException("Not implemented");
	}

	@Override
	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getAuthType() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getContextPath() {
		return context.getContextPath();
	}

	@Override
	public Cookie[] getCookies() {
		if (cookieArr == null)
		{
			cookieArr = cookies.toArray(new Cookie[cookies.size()]);
		}
		return cookieArr;
	}

	@Override
	public long getDateHeader(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getHeader(String arg0) {
		throw new UtilException("Not implemented");
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return new IteratorEnumerator<String>(headers.iterator());
	}

	@Override
	public Enumeration<String> getHeaders(String s) {
		return new IteratorEnumerator<String>(headers.get(s).iterator());
	}

	@Override
	public int getIntHeader(String arg0) {
		if (!headers.contains(arg0.toLowerCase()))
			return 0;
		return Integer.parseInt(headers.get(arg0.toLowerCase()).get(0));
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getPathInfo() {
		String up = uri.getPath();
		String ret = up.replace(getContextPath()+getServletPath(), "");
		int idx = ret.indexOf("?");
		if (idx >= 0)
			ret = ret.substring(0, idx);
		return ret;
	}

	@Override
	public String getPathTranslated() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getQueryString() {
		return uri.getQuery();
	}

	@Override
	public String getRemoteUser() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getRequestURI() {
		String ru = rawUri;
		int idx = ru.indexOf('?');
		if (idx != -1)
			ru = ru.substring(0, idx);
		return ru;
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer ret = new StringBuffer(getRequestURI());
		// OpenId4Java needs this ... I'm not sure if it should be here or URI ...
		ret.insert(0, "http://" + headers.get("host").get(0));
		return ret;
	}

	@Override
	public String getRequestedSessionId() {
		throw new UtilException("Not implemented");
	}

	@Override
	public String getServletPath() {
		return context.servletPath;
	}

	@Override
	public HttpSession getSession() {
		return getSession(false);
	}

	@Override
	public HttpSession getSession(boolean createIfNeeded) {
		if (session != null)
			return session;
		if (!createIfNeeded)
			return null;
		GPHttpSession ret = context.newSession();
		// Should use addCookie really, but I can't be bothered ...
		response.addHeader("Set-Cookie", "JSESSIONID="+ret.cookie+"; Path=" + context.getContextPath());
		return ret;
	}

	@Override
	public Principal getUserPrincipal() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UtilException("Not implemented");
	}

	@Override
	public boolean isUserInRole(String arg0) {
		throw new UtilException("Not implemented");
	}

	public void setResponse(GPResponse response) {
		this.response = response;
	}

	public boolean isForServlet() {
		return getRequestURI().startsWith(getContextPath()+getServletPath());
	}

	public InputStream getStaticResource() {
		return context.getResourceAsStream(getPathInfo()); 
	}
}
