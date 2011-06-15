package com.gmmapowell.quickbuild.config;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.exceptions.QuickBuildException;

public class ProxyCommand extends SpecificChildrenParent<ConfigApplyCommand> implements ConfigApplyCommand {
	private List<ConfigApplyCommand> settings = new ArrayList<ConfigApplyCommand>();
	private String host;
	private int port;
	private String user;
	private String password;

	@SuppressWarnings("unchecked")
	public ProxyCommand(TokenizedLine toks) {
		super(ProxySettingCommand.class);
	}

	@Override
	public void addChild(ConfigApplyCommand obj) {
		settings.add(obj);
	}

	public void setHost(String host) {
		if (this.host != null)
			throw new QuickBuildException("Cannot specify proxy host more than once for a given IP range");
		this.host = host;
	}

	public void setPort(int port) {
		if (this.port != -1)
			throw new QuickBuildException("Cannot specify proxy port more than once for a given IP range");
		this.port = port;
	}

	public void setUser(String user) {
		if (this.user != null)
			throw new QuickBuildException("Cannot specify proxy user more than once for a given IP range");
		this.user = user;
	}

	public void setPassword(String password) {
		if (this.password != null)
			throw new QuickBuildException("Cannot specify proxy password more than once for a given IP range");
		this.password = password;
	}

	@Override
	public void applyTo(Config config) {
		
	}

}
