package com.gmmapowell.quickbuild.build.java;

import java.util.regex.Pattern;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ExcludeCommand extends NoChildCommand implements ConfigApplyCommand {
	private String exclude;
	
	public ExcludeCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "exclude", "pattern"));
	}

	@Override
	public void applyTo(Config config) {
		// TODO Auto-generated method stub
		
	}

	public Pattern getPattern() {
		return Pattern.compile(".*"+exclude.toLowerCase() + ".*");
	}

}
