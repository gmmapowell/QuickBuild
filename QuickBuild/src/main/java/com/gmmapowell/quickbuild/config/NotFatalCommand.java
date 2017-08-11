package com.gmmapowell.quickbuild.config;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;

public class NotFatalCommand extends NoChildCommand implements ConfigApplyCommand {
	public NotFatalCommand(TokenizedLine line) {
		// don't have any arguments
	}

	@Override
	public void applyTo(Config config) {
		// don't have any config
	}
}
