package com.gmmapowell.quickbuild.build.ziniki;

import java.io.File;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.AbstractStrategem;
import com.gmmapowell.quickbuild.core.Strategem;

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
	public File rootDirectory() {
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
