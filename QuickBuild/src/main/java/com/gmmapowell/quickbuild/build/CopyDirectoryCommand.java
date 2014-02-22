package com.gmmapowell.quickbuild.build;

import java.io.File;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategemTactic;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.CloningResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class CopyDirectoryCommand extends AbstractStrategemTactic {
	private String rootDirectoryName;
	private String fromResourceName;
	private String toPath;
	private PendingResource fromResource;
	private CloningResource toResource;
	private final File rootDirectory;
	private StructureHelper files;
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();

	public CopyDirectoryCommand(TokenizedLine toks) {
		// TODO: want 4 args
		super(toks,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "rootDirectoryName", "root"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "fromResourceName", "from resource"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "toPath", "destination")
		);
		rootDirectory = FileUtils.relativePath(rootDirectoryName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		files = new StructureHelper(rootDirectory, "");
		fromResource = new PendingResource(fromResourceName);
		toResource = new CloningResource(this, fromResource, files.getRelative(toPath));
		builds.add(toResource);
		return this;
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		try
		{
			FileUtils.assertDirectory(fromResource.getPath());
			FileUtils.copyRecursive(fromResource.getPath(), toResource.getPath());
			cxt.builtResource(toResource);
			return BuildStatus.SUCCESS;
		}
		catch (Exception ex)
		{
			cxt.failure(null, null, null);
			return BuildStatus.BROKEN;
		}
	}

	@Override
	public String toString() {
		return "Copy " + fromResource + " to " + toResource;
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
		return builds;
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
		return false;
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}
}

