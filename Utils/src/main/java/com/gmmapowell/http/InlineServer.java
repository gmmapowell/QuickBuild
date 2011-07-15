package com.gmmapowell.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

public class InlineServer {
	public static final Logger logger = Logger.getLogger("InlineServer");

	private final int port;
	private final String servletClass;
	protected final GPServletConfig config = new GPServletConfig();

	private HttpServlet servletImpl;

	public InlineServer(int port, String servletClass) {
		this.port = port;
		this.servletClass = servletClass;
	}

	public void setContextPath(String path) {
		((GPServletContext)config.getServletContext()).setContextPath(path);
	}

	public void setServletPath(String path) {
		((GPServletContext)config.getServletContext()).setServletPath(path);
	}

	public void initParam(String key, String value) {
		config.initParam(key, value);
	}

	public void run() {
		run(true);
	}

	public void run(boolean doLoop) {
		try
		{
			ServerSocket s = new ServerSocket(port);
			logger.info("Listening on port " + port);
			Class<?> forName = Class.forName(servletClass);
			servletImpl = (HttpServlet) forName.newInstance();
			servletImpl.init(config);
			while (doLoop)
			{
				Socket conn = s.accept();
				logger.info("Accepting connection request and dispatching to thread");
				new ConnectionThread(this, conn).start();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException
	{
		servletImpl.service(req, resp);
	}
}
