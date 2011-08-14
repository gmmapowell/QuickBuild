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

public class CopyResourceCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem, Tactic {
	private String fromResourceName;
	private String toPath;
	private PendingResource fromResource;
	private CloningResource toResource;
	private StructureHelper files;
	private final ResourcePacket<PendingResource> needed = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();

	@SuppressWarnings("unchecked")
	public CopyResourceCommand(TokenizedLine toks) {
		// TODO: want 4 args
		toks.process(this,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "fromResourceName", "from resource"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "toPath", "destination")
		);
	}

	@Override
	public Strategem applyConfig(Config config) {
		files = new StructureHelper(FileUtils.getCurrentDir(), "");
		fromResource = new PendingResource(fromResourceName);
		toResource = new CloningResource(this, fromResource, new File(files.getRelative(toPath), new File(fromResourceName).getName()));
		builds.add(toResource);
		needed.add(fromResource);
		return this;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
	}

	@Override
	public List<? extends Tactic> tactics() {
		return CollectionUtils.listOf((Tactic)this);
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		try
		{
			BuildResource br = toResource.getActual();
			FileUtils.assertDirectory(br.getPath().getParentFile());
			FileUtils.copy(fromResource.getPath(), br.getPath());
			cxt.builtResource(br, false);
			return BuildStatus.SUCCESS;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			cxt.failure(null, null, null);
			return BuildStatus.BROKEN;
		}
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
		return needed;
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
		return FileUtils.getCurrentDir();
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
		return false;
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}

}

