package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.BuildContext;
import com.gmmapowell.quickbuild.build.BuildStatus;
import com.gmmapowell.quickbuild.config.AbstractBuildCommand;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class AdbInstallCommand extends AbstractBuildCommand {
	private String root;
	private String resource;
//	private String emulator;
	private AndroidContext acxt;
	private StructureHelper files;
	private PendingResource apk;
	private File rootDir;
	private final ResourcePacket<PendingResource> needs = new ResourcePacket<PendingResource>();
	private final ResourcePacket<BuildResource> builds = new ResourcePacket<BuildResource>();
	private AdbInstalledResource buildsInstalled;

	@SuppressWarnings("unchecked")
	public AdbInstallCommand(TokenizedLine toks) {
		toks.process(this,
				new ArgumentDefinition("*", Cardinality.REQUIRED, "root", "root dir"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "apk resource"),
				new ArgumentDefinition("-emulator", Cardinality.OPTION, "emulator", "use emulator"));
		rootDir = FileUtils.relativePath(new File(root));
	}

	@Override
	public Strategem applyConfig(Config config) {
		super.handleOptions(config);
		acxt = config.getAndroidContext();
		apk = new PendingResource(resource);
		needs.add(apk);
		buildsInstalled = new AdbInstalledResource(this, resource);
		builds.add(buildsInstalled);
		return this;
	}
	
	@Override
	public String toString() {
		return "adbinstall " + resource;
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
		return rootDir;
	}

	@Override
	public OrderedFileList sourceFiles() {
		return OrderedFileList.empty();
	}

	@Override
	public String identifier() {
		return "AdbInstall[" + apk.getPending() + "]";
	}

	@Override
	public boolean onCascade() {
		return false;
	}

	@Override
	public boolean analyzeExports() {
		return false;
	}

	@Override
	public BuildStatus execute(BuildContext cxt, boolean showArgs, boolean showDebug) {
		AdbCommand cmd = new AdbCommand(acxt, this, files, apk, buildsInstalled);
		cmd.reinstall();
		return cmd.execute(cxt, showArgs, showDebug);
	}
}
