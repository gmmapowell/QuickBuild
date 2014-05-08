package com.gmmapowell.http;

import javax.servlet.http.HttpServlet;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class RequestThread extends Thread {
	private final ConnectionHandler connectionHandler;
	private final GPRequest request;
	private final GPResponse response;

	public RequestThread(ConnectionHandler connectionHandler, GPRequest request, GPResponse response) {
		this.connectionHandler = connectionHandler;
		this.request = request;
		this.response = response;
	}

	@Override
	public void run() {
		boolean doFinally = true;
		try {
			HttpServlet servlet = request.getServlet();
			InlineServer.logger.debug("Request URI: " + request.getRequestURI() + " - " + servlet);
			if (servlet != null)
			{
				InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Handling through servlet");
				servlet.service(request, response);
				InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Finished servlet handling");
				if (response.isWebSocket()) {
					if (response.getStatus() == 0)
						response.setStatus(101, "Web Socket Protocol Handshake");
					response.commit();
					try
					{
						response.getWriter().flush();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					if (request.wshandler == null)
						throw new UtilException("No websocket handler has been set on InlineServer");
					connectionHandler.useWebSocket();
					doFinally = false;
					return;
				}
			}
			else if (request.getMethod().equalsIgnoreCase("GET")) {
				GPStaticResource staticResource = request.getStaticResource();
				String pathInfo = request.getPathInfo();
				if (staticResource != null)
				{
					InlineServer.logger.debug(Thread.currentThread().getName()+ ": " +"Found static resource");
					response.setStatus(200);
					response.setContentLength((int)staticResource.len);
					guessContentType(response, pathInfo);
					FileUtils.copyStream(staticResource.stream, response.getOutputStream());
					staticResource.close();
				}
				else
				{
					InlineServer.logger.info(Thread.currentThread().getName()+ ": " +"404: Not found - " + pathInfo);
					response.setStatus(404);
				}
			}
			connectionHandler.sendResponse();
		} catch (Exception e) {
			connectionHandler.handleError(e);
		} finally {
			if (doFinally)
				connectionHandler.andFinally();
		}
	}

	public void guessContentType(GPResponse response, String pathInfo) {
		if (pathInfo.endsWith(".html"))
			response.setContentType("text/html");
		else if (pathInfo.endsWith(".js"))
			response.setContentType("application/javascript");
		else if (pathInfo.endsWith(".css"))
			response.setContentType("text/css");
	}
}
