package com.gmmapowell.quickbuild.config;

import java.io.File;

import org.zinutils.parser.TokenizedLine;

public class SetPathCommand extends SetVarCommand {
	private File path;
	
	public SetPathCommand(TokenizedLine toks) {
		super(toks);
		path = new File(var);
	}

	@Override
	public void applyTo(Config config) {
		config.setFileProperty(name, path);
	}

}
