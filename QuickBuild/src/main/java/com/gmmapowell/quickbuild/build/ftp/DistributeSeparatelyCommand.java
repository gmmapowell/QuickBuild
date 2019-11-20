package com.gmmapowell.quickbuild.build.ftp;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class DistributeSeparatelyCommand extends NoChildCommand implements ConfigApplyCommand {
	public DistributeSeparatelyCommand(TokenizedLine toks)
	{
	}

	@Override
	public void applyTo(Config config) {
	}

	@Override
	public String toString() {
		return "Separately";
	}
}
