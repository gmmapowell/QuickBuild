package com.gmmapowell.quickbuild.build.ziniki;

import java.io.File;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategem;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.quickbuild.core.ResourcePacket;
import com.gmmapowell.quickbuild.core.Strategem;
import com.gmmapowell.quickbuild.core.Tactic;
import com.gmmapowell.utils.OrderedFileList;

public class ZinikiCommand extends AbstractStrategem {

	public ZinikiCommand(TokenizedLine toks) {
		super(toks);
	}

	@Override
	public Strategem applyConfig(Config config) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String identifier() {
		// TODO Auto-generated method stub
		return null;
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
	public OrderedFileList sourceFiles() {
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
	public void addChild(ConfigApplyCommand obj) {
		// TODO Auto-generated method stub
		
	}

}
