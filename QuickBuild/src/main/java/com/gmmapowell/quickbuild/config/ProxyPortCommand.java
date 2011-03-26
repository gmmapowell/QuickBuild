package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;

public class ProxyPortCommand extends NoChildCommand implements ProxySettingCommand {

	public ProxyPortCommand(TokenizedLine toks) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void applyTo(ProxyCommand cmd) {
		cmd.setPort(0);
	}

	@Override
	public void applyTo(Config config) {
		// TODO Auto-generated method stub
		
	}

}
