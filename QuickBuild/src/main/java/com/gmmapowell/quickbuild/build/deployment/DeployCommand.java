package com.gmmapowell.quickbuild.build.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class DeployCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, Tactic {

	private String target;
	private List<String> artifacts = new ArrayList<String>();
	private List<Tactic> tactics = new ArrayList<Tactic>();
	private ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private Map<String, PendingResource> pendings = new HashMap<String, PendingResource>();
	private Map<String, BuildResource> buildings = new HashMap<String, BuildResource>();
	private final List<ConfigApplyCommand> options = new ArrayList<ConfigApplyCommand>();

	@SuppressWarnings("unchecked")
	public DeployCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "target", "deploy target"),
				new ArgumentDefinition("*", Cardinality.ONE_OR_MORE, "artifacts", "artifacts")
		);
	}


	@Override
	public void addChild(ConfigApplyCommand obj) {
		options.add(obj);
	}

	@Override
	public Strategem applyConfig(Config config) {
		for (String i : artifacts)
		{
			PendingResource pr = new PendingResource(i);
			pendings.put(i, pr);
			needs.add(pr);
			
			DeployedObject dobj = new DeployedObject(this, FileUtils.relativePath(new File(config.getPath(target), i)));
			buildings.put(i, dobj);
			builds.add(dobj);
		}
		
		for (ConfigApplyCommand cmd : options)
		{
			if (cmd instanceof AfterCommand)
				needs.add(((AfterCommand)cmd).getAfter());
			else
				throw new UtilException("Unrecognized Command: " + cmd);
		}

		tactics.add(this);
		return this;
	}

	@Override
	public Strategem belongsTo() {
		return this;
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
			FileUtils.copyAssertingDirs(from, new File(deployTo, i));
			cxt.builtResource(buildings.get(i));
		}
		return BuildStatus.SUCCESS;
	}

	@Override
	public String identifier() {
		return "Deploy["+target+"]";
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
	public String toString() {
		return identifier();
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}

	private Set <Tactic> procDeps = new HashSet<Tactic>();
	
	@Override
	public void addProcessDependency(Tactic earlier) {
		procDeps.add(earlier);
	}
	
	public Set<Tactic> getProcessDependencies() {
		return procDeps;
	}
}
