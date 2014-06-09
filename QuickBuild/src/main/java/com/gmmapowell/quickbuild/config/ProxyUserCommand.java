package com.gmmapowell.quickbuild.config;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;

public class ProxyUserCommand extends NoChildCommand implements ProxySettingCommand {

	public ProxyUserCommand(TokenizedLine toks) {
	}

	@Override
	public void applyTo(ProxyCommand cmd) {
		cmd.setUser(null);
	}

	@Override
	public void applyTo(Config config) {
		
	}

}
