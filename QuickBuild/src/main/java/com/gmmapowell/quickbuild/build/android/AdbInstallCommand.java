package com.gmmapowell.quickbuild.build.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.OrderedFileList;

public class AdbInstallCommand extends NoChildCommand implements ConfigBuildCommand, Strategem {
	private String root;
	private String resource;
//	private String emulator;
	private AndroidContext acxt;
	private StructureHelper files;
	private PendingResource apk;
	private File rootDir;

	public AdbInstallCommand(TokenizedLine toks) {
		toks.process(this,
				new ArgumentDefinition("*", Cardinality.REQUIRED, "root", "root dir"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "apk resource"),
				new ArgumentDefinition("-emulator", Cardinality.OPTION, "emulator", "use emulator"));
		rootDir = FileUtils.relativePath(new File(root));
	}

	@Override
	public Strategem applyConfig(Config config) {
		acxt = config.getAndroidContext();
		apk = new PendingResource(resource);
		return this;
	}
	

	@Override
	public Collection<? extends Tactic> tactics() {
		List<Tactic> ret = new ArrayList<Tactic>();
		AdbCommand cmd = new AdbCommand(acxt, this, files, apk);
		cmd.reinstall();
		ret.add(cmd);
		return ret;
	}

	@Override
	public String toString() {
		return "adbinstall " + resource;
	}

	@Override
	public ResourcePacket needsResources() {
		ResourcePacket ret = new ResourcePacket();
		ret.add(apk);
		return ret;
	}

	@Override
	public ResourcePacket providesResources() {
		return new ResourcePacket();
	}

	@Override
	public ResourcePacket buildsResources() {
		return new ResourcePacket();
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
		return "AdbInstall[" + apk.compareAs() + "]";
	}
}
