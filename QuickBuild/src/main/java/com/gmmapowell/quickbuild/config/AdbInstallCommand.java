package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.AdbCommand;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.StructureHelper;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class AdbInstallCommand extends NoChildCommand implements ConfigBuildCommand, Strategem {
	private String projectName;
	private String emulator;
	private final File projectDir;
	private AndroidContext acxt;
	private StructureHelper files;

	public AdbInstallCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "projectName", "jar project"),
				new ArgumentDefinition("-emulator", Cardinality.OPTION, "emulator", "use emulator"));
		projectDir = FileUtils.findDirectoryNamed(projectName);
	}

	@Override
	public Strategem applyConfig(Config config) {
		acxt = config.getAndroidContext();
		files = new StructureHelper(projectDir, config.getOutput());
		return this;
	}
	

	@Override
	public Collection<? extends Tactic> tactics() {
		List<Tactic> ret = new ArrayList<Tactic>();
		AdbCommand cmd = new AdbCommand(acxt, this, files, null);
		cmd.reinstall();
		ret.add(cmd);
		return ret;
	}

	@Override
	public String toString() {
		return "adbinstall " + projectName;
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
		return projectDir;
	}
}
