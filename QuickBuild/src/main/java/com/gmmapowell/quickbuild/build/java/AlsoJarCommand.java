package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;

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
