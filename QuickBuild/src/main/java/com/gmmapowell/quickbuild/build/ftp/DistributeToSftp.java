package com.gmmapowell.quickbuild.build.ftp;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;

public class DistributeToSftp implements DistributeTo {
	private boolean fullyConfigured;
	private File privateKeyPath;
	private String username;
	private String host;
	private int port = 22;
	private String saveAs;
	private String directory;

	public DistributeToSftp(Config config, String directory, String dest) {
		this.directory = directory;
		fullyConfigured = true;
		if (config.hasPath("privatekey")) {
			privateKeyPath = config.getPath("privatekey");
			if (!privateKeyPath.canRead()) {
				System.out.println("cannot sftp: private key file does not exist: " + privateKeyPath);
				fullyConfigured = false;
			}
		} else
		{
			System.out.println("Cannot sftp: no private key");
			fullyConfigured = false;
		}
//		if (config.hasPath("knownhosts"))
//			knownHosts = config.getPath("knownhosts");
//		else
//		{
//			System.out.println("Cannot sftp: no known hosts");
//			fullyConfigured = false;
//		}
		Pattern p = Pattern.compile("sftp:([a-zA-Z0-9_]+)@([a-zA-Z0-9_.]+)(:[0-9]+)?/(.+)");
		Matcher matcher = p.matcher(dest);
		if (!matcher.matches())
			throw new UtilException("Could not match path " + dest);
		
		username = matcher.group(1);
		host = matcher.group(2);
		if (matcher.group(3) != null)
			port = Integer.parseInt(matcher.group(3).substring(1));
		saveAs = matcher.group(4);
		if (!saveAs.endsWith("/"))
			saveAs += "/";
	}

	@Override
	public BuildStatus distribute(boolean showDebug, File local, String remote) throws Exception {
		if (!fullyConfigured)
		{
			System.out.println("Skipping distribute because not fully configured");
			return BuildStatus.SKIPPED;
		}
		String to = saveAs + remote;
		if (showDebug)
			System.out.println("  copying " + local + " to " + to);
		Session s = null;
		try
		{
//			JSch.setLogger(new JSchLogger());
			JSch jsch = new JSch();
			jsch.addIdentity(privateKeyPath.getPath());
//			jsch.setKnownHosts(knownHosts.getPath());
			s = jsch.getSession(username, host, port);
			s.setConfig("StrictHostKeyChecking", "no");
			s.connect();
			ChannelSftp openChannel = (ChannelSftp) s.openChannel("sftp");
			openChannel.connect();
			File parent = new File(to).getParentFile();
//			System.out.println("making directory " + parent);
			makeParentDir(openChannel, parent);
			try {
				openChannel.put(local.getPath(), to);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return BuildStatus.SUCCESS;
		}
		finally {
			if (s != null)
				s.disconnect();
		}
	}

	private void makeParentDir(ChannelSftp openChannel, File parent) {
		if (parent == null)
			return;
		try {
			openChannel.mkdir(parent.getPath());
			return;
		} catch (Exception ex) {
			if (ex.getMessage().equals("Failure"))
				return; // it already exists
		}
		makeParentDir(openChannel, parent.getParentFile());
		try {
			openChannel.mkdir(parent.getPath());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public BuildResource resource(DistributeCommand cmd) {
		return new DistributeResource(cmd, directory, port == 22 ? host : (host + ":" + port));
	}

	@Override
	public String toString() {
		return "sftp:" + host + ":" + port;
	}

	public class JSchLogger implements Logger {
		@Override
		public boolean isEnabled(int arg0) {
			return true;
		}

		@Override
		public void log(int arg0, String arg1) {
			System.out.println(arg0 + " " + arg1);
		}
	}
}
