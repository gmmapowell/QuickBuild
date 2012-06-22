package com.gmmapowell.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.serialization.Endpoint;
import com.gmmapowell.utils.FileUtils;

public class InlineServer {
	public static final Logger logger = Logger.getLogger("InlineServer");

	private final int port;
	private final List<File> staticPaths = new ArrayList<File>();
	
	// There is a list of servlets, but, by default, there is only one, the first in the list
	private final List<GPServletDefn> servlets = new ArrayList<GPServletDefn>();

	private Endpoint alertEP;

	private final List<NotifyOnServerReady> interestedParties = new ArrayList<NotifyOnServerReady>();

	private boolean doLoop;

	private Throwable failure;

	private Thread inThread;

	private GPServletConfig staticConfig = new GPServletConfig(this, null);

	public InlineServer(int port, String servletClass) {
		this.port = port;
		servlets.add(new GPServletDefn(this, servletClass));
		inThread = Thread.currentThread();
	}

	public GPServletDefn addServlet(String contextPath, String servletPath, String servletClass) {
		GPServletDefn defn = new GPServletDefn(this, servletClass);
		defn.setContextPath(contextPath);
		defn.setServletPath(servletPath);
		servlets.add(defn);
		return defn;
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
		servlets.get(0).setContextPath(path);
	}

	public void setServletPath(String path) {
		servlets.get(0).setServletPath(path);
	}

	public void initParam(String key, String value) {
		servlets.get(0).initParam(key, value);
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
			for (GPServletDefn servlet : servlets)
				servlet.init();
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

	public void service(GPRequest req, GPResponse resp) throws ServletException, IOException {
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

	public GPServletDefn getServlet(GPRequest request) {
		return null;
	}

	public GPRequest requestFor(String s, InputStream is) throws URISyntaxException {
		String[] command = s.split(" ");
		String method = command[0];
		String rawUri = command[1];

		for (GPServletDefn sd : servlets)
			if (sd.isForMe(rawUri))
				return new GPRequest(sd.getConfig(), method, rawUri, is);

		return new GPRequest(staticConfig , method, rawUri, is);
	}
}
