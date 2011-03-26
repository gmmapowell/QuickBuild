package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;

public class ProxyHostCommand extends NoChildCommand implements ProxySettingCommand {

	public ProxyHostCommand(TokenizedLine toks) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void applyTo(ProxyCommand cmd) {
		cmd.setHost(null);
	}

	@Override
	public void applyTo(Config config) {
		// TODO Auto-generated method stub
		
	}

}
