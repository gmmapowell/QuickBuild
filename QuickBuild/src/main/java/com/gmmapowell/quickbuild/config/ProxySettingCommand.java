package com.gmmapowell.quickbuild.config;

public interface ProxySettingCommand extends ConfigApplyCommand {
	void applyTo(ProxyCommand cmd);
}
