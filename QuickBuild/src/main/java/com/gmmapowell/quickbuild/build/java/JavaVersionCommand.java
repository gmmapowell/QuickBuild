package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class JavaVersionCommand extends NoChildCommand implements ConfigApplyCommand {
	private final String version;

	public JavaVersionCommand(TokenizedLine toks)
	{
		version = toks.tokens[1];
	}

	@Override
	public void applyTo(Config config) {
		if (config.isTopLevel()) {
			// set the property at a global level
			config.setVarProperty("javaVersion", version);
		}
	}

	public String getVersion() {
		return version;
	}
}
