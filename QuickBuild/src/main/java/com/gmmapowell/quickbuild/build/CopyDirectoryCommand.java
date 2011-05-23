package com.gmmapowell.quickbuild.build;

import java.io.File;
import java.util.List;

import com.gmmapowell.collections.CollectionUtils;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class CopyDirectoryCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, Tactic {
	private String rootDirectoryName;
	private String fromResourceName;
	private String toResourceName;
	private PendingResource fromResource;
	private CloningResource toResource;
	private final File rootDirectory;
	private StructureHelper files;
	private BuildResource actualTo;

	@SuppressWarnings("unchecked")
	public CopyDirectoryCommand(TokenizedLine toks) {
		// TODO: want 4 args
		toks.process(this,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "rootDirectoryName", "root"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "fromResourceName", "from resource"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "toResourceName", "destination")
		);
		rootDirectory = FileUtils.relativePath(rootDirectoryName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		files = new StructureHelper(rootDirectory, "");
		fromResource = new PendingResource(fromResourceName);
		toResource = new CloningResource(this, files.getRelative(toResourceName));
		return this;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<? extends Tactic> tactics() {
		return CollectionUtils.listOf((Tactic)this);
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		BuildResource from = cxt.getPendingResource(fromResource);
		System.out.println(from.getPath());
		FileUtils.assertDirectory(from.getPath());
		actualTo = from.cloneInto(toResource);
		FileUtils.copyRecursive(from.getPath(), actualTo.getPath());
		toResource.wasClonedAs(actualTo);
		cxt.resourceAvailable(actualTo);
		return BuildStatus.SUCCESS;
	}

	@Override
	public String toString() {
		return "Copy " + fromResource + " to " + toResource;
	}

	@Override
	public Strategem belongsTo() {
		return this;
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		// TODO: this should all be resolved in construcotr
		ResourcePacket<PendingResource> ret = new ResourcePacket<PendingResource>();
		ret.add(fromResource);
		return ret;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		return new ResourcePacket<BuildResource>();
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		// TODO: this should all be resolved in construcotr
		ResourcePacket<BuildResource> ret = new ResourcePacket<BuildResource>();
		ret.add(toResource);
		return ret;
	}

	@Override
	public File rootDirectory() {
		return rootDirectory;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return OrderedFileList.empty();
	}

	@Override
	public String identifier() {
		return "CopyTo[" + FileUtils.makeRelative(toResource.getClonedPath()) + "]";
	}

	@Override
	public boolean onCascade() {
		// TODO Auto-generated method stub
		return false;
	}

}

