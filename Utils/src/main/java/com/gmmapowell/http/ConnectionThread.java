package com.gmmapowell.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.gmmapowell.exceptions.UtilException;

public class ConnectionThread extends Thread {
	private final InputStream is;
	private final OutputStream os;
	private final InlineServer inlineServer;

	public ConnectionThread(InlineServer inlineServer, Socket conn) throws IOException {
		this.inlineServer = inlineServer;
		is = conn.getInputStream();
		os = conn.getOutputStream();
	}
	
	@Override
	public void run()
	{
		try {
			String s;
			GPRequest request = null;
			while ((s = readLine()) != null && s.trim().length() > 0)
			{
				InlineServer.logger.fine("Header - " + s);
				if (request == null)
					request = new GPRequest((GPServletContext) inlineServer.config.getServletContext(), s, is);
				else
					request.addHeader(s);
			}
			if (request == null)
				throw new UtilException("There was no incoming request");
			request.endHeaders();
			/* Expect:
INFO: Header - POST /ziniki/resources/login HTTP/1.1
INFO: Header - User-Agent: curl/7.19.7 (universal-apple-darwin10.0) libcurl/7.19.7 OpenSSL/0.9.8l zlib/1.2.3
INFO: Header - Host: localhost:10080
INFO: Header - Accept: text/html
INFO: Header - Content-Type: application/xml
INFO: Header - Content-Length: 59
			 */
			
			GPResponse response = new GPResponse(request, os);
			inlineServer.service(request, response);
			response.commit();
			response.getWriter().flush();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String readLine() throws IOException {
		StringBuilder sb = new StringBuilder();
		int b;
		while ((b = is.read()) != -1 && b != '\n')
			sb.append((char)b);
		for (int i=sb.length();i>0;i--)
		{
			if (sb.charAt(i-1) != '\r' && sb.charAt(i-1) != '\n')
				break;
			sb.deleteCharAt(i-1);
		}
		return sb.toString();
	}
}
