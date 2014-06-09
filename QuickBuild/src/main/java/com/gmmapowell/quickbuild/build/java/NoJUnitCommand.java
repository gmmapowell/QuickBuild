package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class NoJUnitCommand extends NoChildCommand implements ConfigApplyCommand {
	public NoJUnitCommand(TokenizedLine toks)
	{
	}

	@Override
	public void applyTo(Config config) {
		
	}
}
