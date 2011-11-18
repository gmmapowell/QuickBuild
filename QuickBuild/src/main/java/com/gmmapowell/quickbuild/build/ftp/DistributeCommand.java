package com.gmmapowell.quickbuild.build.ftp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
			if (config.hasPath("privatekey"))
				privateKeyPath = config.getPath("privatekey");
			if (config.hasPath("knownhosts"))
				knownHosts = config.getPath("knownhosts");
			Pattern p = Pattern.compile("sftp:([a-z0-9_]+)@([a-z0-9_.]+)/(.+)");
			Matcher matcher = p.matcher(destination);
			if (!matcher.matches())
				throw new UtilException("Could not match path " + destination);
			
			username = matcher.group(1);
			host = matcher.group(2);
			saveAs = matcher.group(3);
			builds.add(new DistributeResource(this, host));
		}
		else
			throw new UtilException("Only sftp is supported for distribute at the moment");
		return this;
	}

	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public String identifier() {
		return "Distribute["+directory+"]";
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
		if (privateKeyPath == null || knownHosts == null)
		{
			System.out.println("Skipping distribute because PKP or KH is not set");
			return BuildStatus.SKIPPED;
		}
		ZipOutputStream os = null;
		try
		{
			WriteThruStream wts = new WriteThruStream();
			os = new ZipOutputStream(wts.getOutputEnd());
			SenderThread thr = new SenderThread(wts);
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
				os.putNextEntry(new ZipEntry(f.getPath()));
				FileUtils.copyFileToStream(g, os);
			}
		}
	}

	public class SenderThread extends Thread {
		private final WriteThruStream wts;
		private boolean broken;

		public SenderThread(WriteThruStream wts) {
			this.wts = wts;
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

	@Override
	public int priority() {
		return 10;
	}

	@Override
	public String toString() {
		return "Distribute[" + host + "]";
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}
}
