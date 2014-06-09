package com.gmmapowell.quickbuild.config;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;

public class ProxyHostCommand extends NoChildCommand implements ProxySettingCommand {

	public ProxyHostCommand(TokenizedLine toks) {
	}

	@Override
	public void applyTo(ProxyCommand cmd) {
		cmd.setHost(null);
	}

	@Override
	public void applyTo(Config config) {
		
	}

}
