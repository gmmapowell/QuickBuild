package com.gmmapowell.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

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
			GPServletContext servletContext = (GPServletContext) inlineServer.config.getServletContext();
			String s;
			GPRequest request = null;
			while ((s = readLine()) != null && s.trim().length() > 0)
			{
				InlineServer.logger.info("Header - " + s);
				if (request == null)
					request = new GPRequest(servletContext, s, is);
				else
					request.addHeader(s);
			}
			if (request == null)
				throw new UtilException("There was no incoming request");
			request.endHeaders();
			
			GPResponse response = new GPResponse(request, os);
			if (request.isForServlet())
				inlineServer.service(request, response);
			else {
				InputStream staticResource = request.getStaticResource();
				if (staticResource != null)
				{
					response.setStatus(200);
					FileUtils.copyStream(staticResource, response.getOutputStream());
				}
				else
					response.setStatus(404);
			}
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
