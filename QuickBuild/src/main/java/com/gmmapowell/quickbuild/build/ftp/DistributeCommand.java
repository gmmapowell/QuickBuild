package com.gmmapowell.quickbuild.build.ftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.AbstractBuildCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.FloatToEnd;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;
import com.gmmapowell.utils.WriteThruStream;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class DistributeCommand extends AbstractBuildCommand implements ConfigBuildCommand, Strategem, Tactic, FloatToEnd {
	private String directory;
	private String destination;
	private final List<Tactic> tactics = new ArrayList<Tactic>();
	private File execdir;
	private File privateKeyPath;
	private final ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> provides = new ResourcePacket<BuildResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private File fromdir;
	private String host;
	private String username;
	private String saveAs;
	private File knownHosts;
	private String wrapIn = "";
	private String method;
	private String bucket;
	private boolean fullyConfigured;

	@SuppressWarnings("unchecked")
	public DistributeCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "directory", "directory"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "destination", "destination"));
		tactics.add(this);
	}

	@Override
	public Strategem applyConfig(Config config) {
		execdir = FileUtils.getCurrentDir();
		super.handleOptions(config);

		fromdir = FileUtils.combine(execdir, directory);
		if (destination.startsWith("sftp:"))
		{
			method = "sftp";
			fullyConfigured = true;
			if (config.hasPath("privatekey"))
				privateKeyPath = config.getPath("privatekey");
			else
			{
				System.out.println("Cannot sftp: no private key");
				fullyConfigured = false;
			}
			if (config.hasPath("knownhosts"))
				knownHosts = config.getPath("knownhosts");
			else
			{
				System.out.println("Cannot sftp: no known hosts");
				fullyConfigured = false;
			}
			Pattern p = Pattern.compile("sftp:([a-zA-Z0-9_]+)@([a-zA-Z0-9_.]+)/(.+)");
			Matcher matcher = p.matcher(destination);
			if (!matcher.matches())
				throw new UtilException("Could not match path " + destination);
			
			username = matcher.group(1);
			host = matcher.group(2);
			saveAs = matcher.group(3);
			builds.add(new DistributeResource(this, host));
		}
		else if (destination.startsWith("s3:"))
		{
			method = "s3";
			fullyConfigured = true;
			if (config.hasPath("awspath"))
				privateKeyPath = config.getPath("awspath");
			else
			{
				System.out.println("Cannot s3: no aws setup");
				fullyConfigured = false;
			}
			Pattern p = Pattern.compile("s3:([a-zA-Z0-9_]+)(.s3.amazonaws.com)/(.+)");
			Matcher matcher = p.matcher(destination);
			if (!matcher.matches())
				throw new UtilException("Could not match s3 path " + destination);
			
			bucket = matcher.group(1);
			host = matcher.group(1)+matcher.group(2);
			saveAs = matcher.group(3);
			builds.add(new DistributeResource(this, host));
		}
		else
			throw new UtilException("Unrecognized protocol in " + destination +". Supported protocols are: sftp, s3");
		return this;
	}

	
	@Override
	public boolean handleOption(Config config, ConfigApplyCommand opt) {
		if (super.handleOption(config, opt))
			return true;
		else if (opt instanceof DistributeWrapCommand)
		{
			wrapIn = ((DistributeWrapCommand) opt).getWrap();
			if (!wrapIn.endsWith("/"))
				wrapIn += "/";
		}
		else
			return false;
		return true;
	}

	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public String identifier() {
		return "Distribute[" + host + "-" + saveAs + "]";
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needs;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return provides;
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return builds;
	}

	@Override
	public File rootDirectory() {
		return execdir;
	}

	@Override
	public List<? extends Tactic> tactics() {
		return tactics;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return new OrderedFileList();
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (!fullyConfigured)
		{
			System.out.println("Skipping distribute because not fully configured");
			return BuildStatus.SKIPPED;
		}
		ZipOutputStream os = null;
		try
		{
			WriteThruStream wts = new WriteThruStream();
			os = new ZipOutputStream(wts.getOutputEnd());
			SenderThread thr;
			if (method.equals("sftp"))
				thr = new SftpSenderThread(wts);
			else if (method.equals("s3"))
				thr = new S3SenderThread(wts);
			else
				throw new UtilException("Unrecognized distribute method: " + method);
			thr.start();
			sendFilesTo(os);
			os.close();
			thr.join();
			builds.provide(cxt, false);
			if (thr.broken)
				return BuildStatus.BROKEN;
		}
		catch (Exception ex)
		{
			try
			{
				if (os != null)
					os.close();
			}
			catch (Exception e2) { } 
			ex.printStackTrace();
			return BuildStatus.BROKEN;
		}
		return BuildStatus.SUCCESS;
	}

	private void sendFilesTo(ZipOutputStream os) throws IOException {
		for (File f : FileUtils.findFilesUnderMatching(fromdir, "*"))
		{
			File g = FileUtils.relativePath(fromdir, f.getPath());
//			System.out.println("Sending " + g + " => " + g.isDirectory());
			if (g.isDirectory())
				continue;
			else
			{
				os.putNextEntry(new ZipEntry(wrapIn + FileUtils.posixPath(f)));
				FileUtils.copyFileToStream(g, os);
			}
		}
	}

	@Override
	public int priority() {
		return 10;
	}

	@Override
	public String toString() {
		return "Distribute[" + host + "-" + saveAs + "]";
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}

	public class SenderThread extends Thread
	{
		protected final WriteThruStream wts;
		protected boolean broken;
		
		public SenderThread(WriteThruStream wts) {
			this.wts = wts;
		}
	}

	public class S3SenderThread extends SenderThread {
		public S3SenderThread(WriteThruStream wts) {
			super(wts);
		}

		@Override
		public void run() {
			try
			{
				// We need to know the length so storing to a temp file is easiest
				InputStream is = wts.getInputEnd();
				File tmp = FileUtils.copyStreamToTempFile(is);
				AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(privateKeyPath));
				s3.putObject(bucket, saveAs, tmp);
				is.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				wts.cancel();
				broken = true;
			}
		}
	}

	public class SftpSenderThread extends SenderThread {

		public SftpSenderThread(WriteThruStream wts) {
			super(wts);
		}

		@Override
		public void run() {
			try
			{
				JSch jsch = new JSch();
				jsch.addIdentity(privateKeyPath.getPath());
				jsch.setKnownHosts(knownHosts.getPath());
				Session s = jsch.getSession(username, host);
				s.connect();
				ChannelSftp openChannel = (ChannelSftp) s.openChannel("sftp");
				openChannel.connect();
				openChannel.put(wts.getInputEnd(), saveAs);
				s.disconnect();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				wts.cancel();
				broken = true;
			}
		}
	}
}
