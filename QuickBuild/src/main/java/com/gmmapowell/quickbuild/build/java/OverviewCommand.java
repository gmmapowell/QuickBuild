package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

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
