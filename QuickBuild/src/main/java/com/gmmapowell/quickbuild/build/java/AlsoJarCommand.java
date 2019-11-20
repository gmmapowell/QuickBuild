package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class AlsoJarCommand extends NoChildCommand implements ConfigApplyCommand {
	public AlsoJarCommand(TokenizedLine toks)
	{
	}

	@Override
	public void applyTo(Config config) {
	}
}
