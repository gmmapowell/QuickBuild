package com.gmmapowell.http;

import java.io.File;
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

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.serialization.Endpoint;
import com.gmmapowell.utils.FileUtils;

public class InlineServer {
	public static final Logger logger = Logger.getLogger("InlineServer");

	private final int port;
	private final String servletClass;
	protected final GPServletConfig config = new GPServletConfig(this);
	private final List<File> staticPaths = new ArrayList<File>();

	private HttpServlet servletImpl;
	private Endpoint alertEP;

	private final List<NotifyOnServerReady> interestedParties = new ArrayList<NotifyOnServerReady>();

	private boolean doLoop;

	private Throwable failure;

	private Thread inThread;

	public InlineServer(int port, String servletClass) {
		this.port = port;
		this.servletClass = servletClass;
		inThread = Thread.currentThread();
	}

	public void addFailure(Throwable ex) {
		if (failure == null)
			failure = ex;
	}

	public Throwable getFailure()
	{
		return failure;
	}
	
	public void setContextPath(String path) {
		servletContext().setContextPath(path);
	}

	public void setServletPath(String path) {
		servletContext().setServletPath(path);
	}

	public GPServletContext servletContext() {
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
		if (inThread != Thread.currentThread())
			throw new UtilException("Cannot run in different thread to creation thread");
		int timeout = 10;
		ServerSocket s = null;
		try {
			this.doLoop = wantLoop;
			s = new ServerSocket(port);
			s.setSoTimeout(timeout);
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
					if (timeout < 2000)
					{
						timeout *= 2;
						s.setSoTimeout(timeout);
						logger.finest("Timeout now = " + timeout);
					}
				}
			}
			s.close();
			logger.info("Server exiting");
		} catch (Exception ex) {
			ex.printStackTrace();
			failure = ex;
			if (s != null)
			{
				try {
					s.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
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
		inThread.interrupt();
	}

	public void addStaticDir(File file) {
		staticPaths.add(file);
	}

	public List<File> staticPaths() {
		if (staticPaths.isEmpty())
			staticPaths.add(FileUtils.getCurrentDir().getAbsoluteFile());
		return staticPaths;
	}
}
