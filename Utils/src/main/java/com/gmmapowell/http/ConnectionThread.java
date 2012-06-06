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
	private boolean closeConnection;
	private boolean isWebSocket;

	public ConnectionThread(InlineServer inlineServer, Socket conn) throws IOException {
		this.inlineServer = inlineServer;
		is = conn.getInputStream();
		os = conn.getOutputStream();
	}
	
	@Override
	public void run()
	{
		GPResponse response = null;
		closeConnection = true;
		isWebSocket = false;
		InlineServer.logger.fine(Thread.currentThread().getName()+ ": " + "Processing Incoming Request");
		try {
			for (;;) {
				try
				{
					response = handleOneRequest();
					if (closeConnection)
						break;
				}
				finally
				{
					if (response != null)
					{
						if (response.getStatus() == 0)
						{
							if (isWebSocket)
								response.setStatus(101, "Web Socket Protocol Handshake");
							else if (closeConnection)
								response.setStatus(200, "OK");
						}
						// This is an attempt to handle "suspended" connections ... but what is the right way to do it?
						// i.e. how should we tell?
						if (response.getStatus() != 0)
						{
							response.commit();
							try
							{
								response.getWriter().flush();
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			InlineServer.logger.fine(Thread.currentThread().getName()+ ": " +"Closing Connection");
			try
			{
				os.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private GPResponse handleOneRequest() throws Exception {
		GPResponse response;
		GPServletContext servletContext = (GPServletContext) inlineServer.config.getServletContext();
		String s;
		GPRequest request = null;
		while ((s = readLine()) != null && s.trim().length() > 0)
		{
			InlineServer.logger.fine(Thread.currentThread().getName()+ ": " +"Header - " + s);
			if (request == null)
				request = new GPRequest(servletContext, s, is);
			else
				request.addHeader(s);
		}
		if (request == null)
			throw new UtilException(Thread.currentThread().getName()+ ": " + "There was no incoming request");
		InlineServer.logger.fine(Thread.currentThread().getName()+ ": " +"Done Headers");
		request.endHeaders();
		
		String connhdr;
		{
			connhdr = request.getHeader("connection");
			if (connhdr == null)
				connhdr = "close";
			else if (connhdr.equalsIgnoreCase("keep-alive"))
				closeConnection = false;
			else if (connhdr.equalsIgnoreCase("upgrade"))
				closeConnection = false;
			else if (connhdr.equalsIgnoreCase("close"))
				closeConnection = true;
		}
		{
			String upghdr = request.getHeader("upgrade");
			if (upghdr != null && (upghdr.equalsIgnoreCase("websocket")))
				isWebSocket = true;
		}
		response = new GPResponse(request, os, connhdr);
		if (request.isForServlet())
		{
			InlineServer.logger.fine(Thread.currentThread().getName()+ ": " +"Handling through servlet");
			inlineServer.service(request, response);
			InlineServer.logger.fine(Thread.currentThread().getName()+ ": " +"Finished servlet handling");
		}
		else {
			GPStaticResource staticResource = request.getStaticResource();
			if (staticResource != null)
			{
				InlineServer.logger.fine(Thread.currentThread().getName()+ ": " +"Found static resource");
				response.setStatus(200);
				response.setContentLength((int)staticResource.len);
				FileUtils.copyStream(staticResource.stream, response.getOutputStream());
				staticResource.close();
			}
			else
			{
				InlineServer.logger.fine(Thread.currentThread().getName()+ ": " +"404: Not found - " + request.getPathInfo());
				response.setStatus(404);
			}
		}
		return response;
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
