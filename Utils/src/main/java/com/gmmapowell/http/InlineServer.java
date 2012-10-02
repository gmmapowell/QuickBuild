package com.gmmapowell.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;

public class InlineServer {
	public static final Logger logger = Logger.getLogger("InlineServer");

	private final RemoteIO remote;
	private final List<File> staticPaths = new ArrayList<File>();
	
	// There is a list of servlets, but, by default, there is only one, the first in the list
	private final List<GPServletDefn> servlets = new ArrayList<GPServletDefn>();

	private final List<NotifyOnServerReady> interestedParties = new ArrayList<NotifyOnServerReady>();

	private boolean doLoop;

	private Throwable failure;

	private Thread inThread;

	private GPServletConfig staticConfig = new GPServletConfig(this, null);

	private long totalRequests;

	private int numRequests;

	public InlineServer(int port, String servletClass) {
		this.remote = new RemoteIO.UsingSocket(this, port);
		servlets.add(new GPServletDefn(this, servletClass));
		inThread = Thread.currentThread();
	}

	public InlineServer(String amqpUri, String servletClass) {
		this.remote = new RemoteIO.UsingAMQP(this, amqpUri);
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
		remote.setAlertTo(alert);
	}

	public void run() {
		run(true);
	}

	public void run(boolean wantLoop) {
		if (inThread != Thread.currentThread())
			throw new UtilException("Cannot run in different thread to creation thread");
		try {
			this.doLoop = wantLoop;
			remote.init();
			for (GPServletDefn servlet : servlets)
				servlet.init();
			remote.announce(this.interestedParties);
			HashSet<ConnectionThread> threads = new HashSet<ConnectionThread>();
			while (doLoop) {
				RemoteIO.Connection conn = remote.accept();
				if (conn != null)
				{
					ConnectionThread thr = new ConnectionThread(this, conn);
					logger.fine("Accepting connection request and dispatching to thread " + thr);
					thr.start();
					threads.add(thr);
					for (ConnectionThread ct : threads)
					{
						if (!ct.isAlive())
						{
							threads.remove(ct);
							// It's good enough to break here after we've removed one, because if we remove at least one every time we add one it can't grow indefinitely ...
							break;
						}
					}
				}
			}
			// Wait for all non-dead threads (at least for a while)
			for (ConnectionThread ct : threads)
			{
				if (ct.isAlive())
				{
					logger.info("Joining thread " + ct);
					ct.join(1000);
				}
			}
			logger.info("Closing remote " + remote);
			remote.close();
			for (GPServletDefn servlet : servlets)
				servlet.destroy();
			logger.info("Server exiting");
		} catch (Exception ex) {
			ex.printStackTrace();
			failure = ex;
			if (remote != null)
			{
				try {
					remote.close();
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
		String protocol = command[2];

		for (GPServletDefn sd : servlets)
			if (sd.isForMe(rawUri))
				return new GPRequest(sd.getConfig(), method, rawUri, protocol, is);

		return new GPRequest(staticConfig , method, rawUri, protocol, is);
	}

	public synchronized void requestTime(Date start, Date end) {
		long elapsed = end.getTime()-start.getTime();
		totalRequests += elapsed;
		numRequests++;
		logger.info("Request took " + (elapsed/1000.0) + "; total = " + totalRequests/1000.0 + "; average = " + totalRequests*10/numRequests/10000.0);
	}
}
