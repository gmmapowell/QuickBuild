package com.gmmapowell.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.gmmapowell.collections.ListMap;
import com.gmmapowell.exceptions.UtilException;

public class GPResponse implements HttpServletResponse {

	private int status;
	private String statusMsg;
	private final ListMap<String, String> headers = new ListMap<String, String>();
	private final ServletOutputStream sos;
	private final PrintWriter pw;
	private boolean committed;
	private SimpleDateFormat dateFormat;

	public GPResponse(GPRequest request, OutputStream os) {
		request.setResponse(this);
		pw = new PrintWriter(os);
		sos = new GPServletOutputStream(os);
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public String status() {
		return "HTTP/1.1 " + status + " " + statusMsg;
	}

	@Override
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		commit();
		return sos;
	}


	private void reply(String string) {
		pw.print(string +"\r\n");
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return pw;
	}

	@Override
	public boolean isCommitted() {
		return committed;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setContentLength(int arg0) {
		addIntHeader("Content-Length", arg0);
	}

	@Override
	public void setContentType(String arg0) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void addCookie(Cookie arg0) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void addHeader(String arg0, String arg1) {
		headers.add(arg0, arg1);
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		addHeader(arg0, Integer.toString(arg1));
	}

	@Override
	public boolean containsHeader(String arg0) {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String encodeURL(String arg0) {
		// TODO Auto-generated method stub
		throw new UtilException("Not Implemented");
	}

	@Override
	public String encodeUrl(String arg0) {
		throw new UtilException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public void sendError(int arg0) throws IOException {
		status = arg0;
		statusMsg = null;
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {
		status = arg0;
		statusMsg = arg1;
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setStatus(int arg0) {
		setStatus(arg0, "OK");
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		status = arg0;
		statusMsg = arg1;
	}

	private List<String> sendHeaders() {
		List<String> ret = new ArrayList<String>();
		for (String s : headers.keySet()) {
			if (s.equals("Set-Cookie"))
			{
				for (String v : headers.get(s))
				{
					StringBuilder buf = new StringBuilder();
					buf.append(s);
					buf.append(": ");
					buf.append(v);
					ret.add(buf.toString());
				}
			}
			else
			{
				StringBuilder buf = new StringBuilder();
				buf.append(s);
				String sep = ": ";
				for (String v : headers.get(s))
				{
					buf.append(sep);
					buf.append(v);
					sep = ", ";
				}
				ret.add(buf.toString());
			}
		}
		return ret;
	}

	public void commit() {
		if (!committed)
		{
			reply(status());
			reply("Server: InlineServer/1.1");
			reply("Date: " + dateFormat.format(new Date())); /* Sat, 18 Jun 2011 21:52:27 GMT */
			reply("Connection: close");
			for (String r : sendHeaders())
				reply(r);
			reply("");
			pw.flush();
			committed = true;
		}
	}

	public int getStatus() {
		return status;
	}

}
