package com.gmmapowell.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gmmapowell.serialization.Endpoint;

public class InlineServer {
	public static final Logger logger = Logger.getLogger("InlineServer");

	private final int port;
	private final String servletClass;
	protected final GPServletConfig config = new GPServletConfig();

	private HttpServlet servletImpl;
	private Endpoint alertEP;

	private final List<NotifyOnServerReady> interestedParties = new ArrayList<NotifyOnServerReady>();

	private boolean doLoop;

	public InlineServer(int port, String servletClass) {
		this.port = port;
		this.servletClass = servletClass;
	}

	public void setContextPath(String path) {
		servletContext().setContextPath(path);
	}

	public void setServletPath(String path) {
		servletContext().setServletPath(path);
	}

	private GPServletContext servletContext() {
		return ((GPServletContext) config.getServletContext());
	}

	public void initParam(String key, String value) {
		config.initParam(key, value);
	}

	public void setAlert(String alert) {
		alertEP = Endpoint.parse(alert);
	}

	public void run() {
		run(true);
	}

	public void run(boolean wantLoop) {
		try {
			this.doLoop = wantLoop;
			ServerSocket s = new ServerSocket(port);
			s.setSoTimeout(1000);
			logger.info("Listening on port " + s.getLocalPort());
			Class<?> forName = Class.forName(servletClass);
			servletImpl = (HttpServlet) forName.newInstance();
			servletImpl.init(config);
			Endpoint addr = new Endpoint(s);
			if (alertEP != null) {
				logger.info("Sending " + addr + " to " + alertEP);
				alertEP.send(addr.toString());
			}
			for (NotifyOnServerReady nosr : interestedParties)
				nosr.serverReady(this, addr);
			while (doLoop) {
				try
				{
					Socket conn = s.accept();
					logger.fine("Accepting connection request and dispatching to thread");
					new ConnectionThread(this, conn).start();
				}
				catch (SocketTimeoutException ex)
				{
					// this is perfectly normal ... continue (or not)
				}
			}
			logger.info("Server exiting");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		servletImpl.service(req, resp);
	}

	public void notify(NotifyOnServerReady toNotify) {
		interestedParties.add(toNotify);
	}

	public void pleaseExit() {
		doLoop = false;
	}
}
