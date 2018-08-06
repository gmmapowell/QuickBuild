package com.gmmapowell.quickbuild.build.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.zinutils.exceptions.UtilException;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.OrderedFileList;

import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.AbstractBuildCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ResourceCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.FloatToEnd;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;

public class DistributeCommand extends AbstractBuildCommand implements FloatToEnd {

	private String directory;
	private List<String> destinations = new ArrayList<String>();
	private List<DistributeTo> distributions = new ArrayList<DistributeTo>();
	private File execdir;
	private final ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> provides = new ResourcePacket<BuildResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private File fromdir;
//	private File knownHosts;
	private String wrapIn = "";
	private boolean separately;
	private boolean includeDependencies;
	private Set<PendingResource> resources = new HashSet<PendingResource>();

	@SuppressWarnings("unchecked")
	public DistributeCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "directory", "directory"),
				new ArgumentDefinition("*", Cardinality.ONE_OR_MORE, "destinations", "destination"));
	}

	@Override
	public Strategem applyConfig(Config config) {
		execdir = FileUtils.getCurrentDir();
		super.handleOptions(config);

		fromdir = FileUtils.combine(execdir, directory);
		String destination = destinations.get(0); // should be a for loop
		String sendTo = destination.replaceAll("\\$\\{date}", new SimpleDateFormat("yyyyMMdd").format(new Date()));
		processDestination(config, sendTo);
		return this;
	}

	private void processDestination(Config config, String dest) {
		//		System.out.println("Sending to: " + destination);
		DistributeTo distr;
		if (dest.startsWith("sftp:")) {
			distr = new DistributeToSftp(config, dest);
		} else if (dest.startsWith("s3:")) {
			distr = new DistributeToS3(config, dest);
		} else
			throw new UtilException("Unrecognized protocol in " + dest +". Supported protocols are: sftp, s3");
		distributions.add(distr);
		builds.add(distr.resource(this));
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
		else if (opt instanceof DistributeSeparatelyCommand)
		{
			separately  = true;
		}
		else if (opt instanceof DistributeIncludeDependenciesCommand)
		{
			includeDependencies = true;
		}
		else if (opt instanceof ResourceCommand)
		{
			PendingResource r = ((ResourceCommand)opt).getPendingResource();
			resources.add(r);
			needs.add(r);
		}
		else
			return false;
		return true;
	}

	@Override
	public String identifier() {
		return "Distribute" + destinations;
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
	public OrderedFileList sourceFiles() {
		return new OrderedFileList();
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		if (separately)
		{
			BuildStatus stat = BuildStatus.SUCCESS;
			for (File[] f : sendFiles(cxt)) {
				for (DistributeTo d : distributions)
					try {
						/* ignore possible skipped */ d.distribute(showDebug, f[0], f[1].getPath());
					} catch (Exception ex) {
						ex.printStackTrace();
						stat = BuildStatus.BROKEN;
					}
			}
			return stat;
		}
		else
		{
			ZipOutputStream os = null;
			File f = null;
			try
			{
				f = File.createTempFile("zipfile", ".zip");
				f.deleteOnExit();
				try (FileOutputStream fos = new FileOutputStream(f)) {
					os = new ZipOutputStream(fos);
					for (File[] fs : sendFiles(cxt)) {
						if (fs[0].isDirectory())
							continue;
						os.putNextEntry(new ZipEntry(wrapIn + FileUtils.posixPath(fs[0])));
					}
					os.close();
				}
				for (DistributeTo d : distributions)
					d.distribute(showDebug, f, "");
				builds.provide(cxt, false);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return BuildStatus.BROKEN;
			} finally {
				if (f != null)
					f.delete();
			}
		}
		return BuildStatus.SUCCESS;
	}

	private List<File[]> sendFiles(BuildContext cxt) {
		ArrayList<File[]> ret = new ArrayList<>();
		if (directory.equals("_")) {
			// don't have a specific fromdir, expect resources
			if (resources.isEmpty())
				throw new RuntimeException("Distribute _ requires resources");
			Set<BuildResource> all = new HashSet<>();
			for (PendingResource r : resources) {
				all.add(r);
				for (BuildResource q : cxt.getTransitiveDependencies(r.getBuiltBy())) {
					all.add(q);
				}
			}
			for (BuildResource r : all) {
				File path = r.getPath();
				ret.add(new File[] { path, new File(path.getName()) });
			}
		} else {
			for (File f : FileUtils.findFilesUnderMatching(fromdir, "*"))
			{
				File g = FileUtils.relativePath(fromdir, f.getPath());
				ret.add(new File[] { g, f });
			}
		}
		return ret;
	}

	@Override
	public int priority() {
		return 10;
	}

	@Override
	public String toString() {
		return "Distribute" + distributions;
	}
}
