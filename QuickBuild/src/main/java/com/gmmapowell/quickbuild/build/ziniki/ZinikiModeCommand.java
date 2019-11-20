package com.gmmapowell.quickbuild.build.ziniki;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class ZinikiModeCommand extends NoChildCommand implements ConfigApplyCommand {
	private String mode;

	public ZinikiModeCommand(TokenizedLine toks) {
		mode = toks.cmd();
	}

	@Override
	public void applyTo(Config config) {
	}

	public String getMode() {
		return mode;
	}

}
