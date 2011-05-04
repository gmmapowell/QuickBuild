package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class NoJUnitCommand extends NoChildCommand implements ConfigApplyCommand {
	public NoJUnitCommand(TokenizedLine toks)
	{
	}

	@Override
	public void applyTo(Config config) {
		// TODO Auto-generated method stub
		
	}
}
