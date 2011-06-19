package com.gmmapowell.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.gmmapowell.exceptions.UtilException;

public class GPResponse implements HttpServletResponse {

	private int status;
	private String statusMsg;

	public GPResponse(GPRequest request) {
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
		throw new UtilException("Not Implemented");
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		throw new UtilException("Not Implemented");
	}

	@Override
	public boolean isCommitted() {
		throw new UtilException("Not Implemented");
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
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
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
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
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
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		// TODO Auto-generated method stub

		throw new UtilException("Not Implemented");
	}

}
