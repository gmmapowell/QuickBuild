package com.gmmapowell.quickbuild.build.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;

public class DeployCommand extends AbstractBuildCommand {
	private boolean unnest = false;
	private String target;
	private final List<String> artifacts = new ArrayList<String>();
	private ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private Map<String, PendingResource> pendings = new HashMap<String, PendingResource>();
	private Map<String, BuildResource> buildings = new HashMap<String, BuildResource>();
	private DeployedObject firstBR;

	@SuppressWarnings("unchecked")
	public DeployCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("--unnest", Cardinality.OPTION, "unnest", "do not copy directory tree"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "target", "deploy target"),
				new ArgumentDefinition("*", Cardinality.ONE_OR_MORE, "artifacts", "artifacts")
		);
	}

	@Override
	public Strategem applyConfig(Config config) {
		for (String i : artifacts)
		{
			PendingResource pr = new PendingResource(i);
			pendings.put(i, pr);
			needs.add(pr);
			
			String to = i;
			if (unnest)
				to = new File(to).getName();
			DeployedObject dobj = new DeployedObject(this, FileUtils.relativePath(new File(config.getPath(target), to)));
			if (firstBR == null)
				firstBR = dobj;
			buildings.put(i, dobj);
			builds.add(dobj);
		}
		
		super.handleOptions(config);

		return this;
	}
	
	@Override
	public boolean handleOption(Config config, ConfigApplyCommand cmd) {
		if (super.handleOption(config, cmd))
			return true;
		else if (cmd instanceof AfterCommand) {
			needs.add(((AfterCommand)cmd).getAfter());
			return true;
		} else
			throw new UtilException("Unrecognized Command: " + cmd);
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		File deployTo = cxt.getPath(target);
		if (!deployTo.isDirectory())
		{
			System.out.println("The deploy directory " + deployTo + " for target " + target + " does not exist");
			return BuildStatus.BROKEN;
		}
		if (!deployTo.canWrite())
		{
			System.out.println("The deploy directory " + deployTo + " for target " + target + " is not writable");
			return BuildStatus.BROKEN;
		}
		for (String i : artifacts)
		{
			File from = pendings.get(i).getPath();
			BuildResource br = buildings.get(i);
			FileUtils.copyAssertingDirs(from, br.getPath());
			cxt.builtResource(br);
		}
		return BuildStatus.SUCCESS;
	}

	@Override
	public String identifier() {
		return "Deploy["+firstBR.getPath()+"]";
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		return needs;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return new ResourcePacket<BuildResource>();
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		return builds;
	}

	@Override
	public File rootDirectory() {
		return null;
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
	public String toString() {
		return identifier();
	}
}
