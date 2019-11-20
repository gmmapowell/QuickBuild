package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;

public class DoubleQuickCommand extends NoChildCommand implements ConfigApplyCommand {
	public DoubleQuickCommand(TokenizedLine toks)
	{
		toks.process(this);
	}

	@Override
	public void applyTo(Config config) {
	}
	
	@Override
	public String toString() {
		return "DoubleQuick";
	}
}
