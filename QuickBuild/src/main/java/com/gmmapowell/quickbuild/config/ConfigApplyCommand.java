package com.gmmapowell.quickbuild.config;

public interface ConfigApplyCommand extends ConfigCommand {
	void applyTo(Config config);
}
