package com.gmmapowell.quickbuild.core;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.config.ConfigBuildCommand;
import com.gmmapowell.quickbuild.config.SpecificChildrenParent;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.OrderedFileList;

public abstract class AbstractStrategem extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigBuildCommand, Strategem {
	protected final List<Tactic> tactics = new ArrayList<Tactic>();

	@SuppressWarnings("unchecked")
	public AbstractStrategem(TokenizedLine toks, ArgumentDefinition... args) {
		toks.process(this, args);
	}

	public AbstractStrategem(Class<? extends ConfigApplyCommand>[] clzs) {
		super(clzs);
	}

	@Override
	public final List<? extends Tactic> tactics() {
		return tactics;
	}

	@Override
	public ResourcePacket<PendingResource> needsResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourcePacket<BuildResource> providesResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourcePacket<BuildResource> buildsResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderedFileList sourceFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	@Override
	public void addChild(ConfigApplyCommand obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public File rootDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Tactic> tactics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCascade() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean analyzeExports() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Strategem applyConfig(Config config) {
		// TODO Auto-generated method stub
		return null;
	}
*/
}
