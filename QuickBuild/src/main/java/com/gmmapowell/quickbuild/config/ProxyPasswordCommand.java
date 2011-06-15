package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;

public class ProxyPasswordCommand extends NoChildCommand implements ProxySettingCommand {

	public ProxyPasswordCommand(TokenizedLine toks) {
	}

	@Override
	public void applyTo(ProxyCommand cmd) {
		cmd.setPassword(null);
	}

	@Override
	public void applyTo(Config config) {
		
	}

}
