package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class OverviewCommand extends NoChildCommand implements ConfigApplyCommand {
	boolean exclude;
	String overview;
	
	public OverviewCommand(TokenizedLine toks)
	{
		toks.process(this,
				new ArgumentDefinition("*", Cardinality.REQUIRED, "overview", "package"));
	}

	@Override
	public void applyTo(Config config) {
		
	}
	
	@Override
	public String toString() {
		return "Overview[" +  overview + "]";
	}
}
