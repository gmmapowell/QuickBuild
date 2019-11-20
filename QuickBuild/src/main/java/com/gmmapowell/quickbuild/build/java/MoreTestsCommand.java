package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class MoreTestsCommand extends NoChildCommand implements ConfigApplyCommand {
	private String dir;
	
	public MoreTestsCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "dir", "dir"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	public String getTestDir() {
		return dir;
	}
}
