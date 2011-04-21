package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.Collection;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class CopyDirectoryCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, Tactic {
	private String fromResourceName;
	private String toResourceName;
	private BuildResource fromResource;
	private BuildResource toResource;

	@SuppressWarnings("unchecked")
	public CopyDirectoryCommand(TokenizedLine toks) {
		// TODO: want 4 args
		toks.process(this,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "fromResourceName", "from resource"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "toResourceName", "destination")
		);
	}

	@Override
	public Strategem applyConfig(Config config) {
		fromResource = config.getResourceByName(fromResourceName);
		// TODO: should we create this?
		toResource = config.getResourceByName(toResourceName);
		return this;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<? extends Tactic> tactics() {
		return CollectionUtils.listOf((Tactic)this);
	}

	@Override
	public BuildStatus execute(BuildContext cxt) {
		/* TODO: not my problem
		if (!cxt.requiresBuiltResource(this, fromResource))
			return BuildStatus.RETRY;
			*/
		FileUtils.assertDirectory(toResource.getPath());
		FileUtils.copyRecursive(fromResource.getPath(), toResource.getPath());
		cxt.addBuiltResource(toResource);
		return BuildStatus.SUCCESS;
	}

	@Override
	public String toString() {
		return "Copy " + fromResource + " to " + toResource;
	}

	@Override
	public Strategem belongsTo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourcePacket needsResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourcePacket providesResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourcePacket buildsResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File rootDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

}
