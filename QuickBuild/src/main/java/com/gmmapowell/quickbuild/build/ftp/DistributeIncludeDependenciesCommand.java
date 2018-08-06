package com.gmmapowell.quickbuild.build.ftp;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class DistributeIncludeDependenciesCommand extends NoChildCommand implements ConfigApplyCommand {
	public DistributeIncludeDependenciesCommand(TokenizedLine toks)
	{
	}

	@Override
	public void applyTo(Config config) {
	}

	@Override
	public String toString() {
		return "IncludeDependencies";
	}
}
