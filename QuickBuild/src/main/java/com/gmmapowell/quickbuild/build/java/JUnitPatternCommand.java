package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class JUnitPatternCommand extends NoChildCommand implements ConfigApplyCommand {
	private String pattern;
	
	public JUnitPatternCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "pattern", "pattern to match"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	public String getPattern() {
		return pattern;
	}

}
