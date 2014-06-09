package com.gmmapowell.quickbuild.build.java;

import java.util.regex.Pattern;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class ExcludeCommand extends NoChildCommand implements ConfigApplyCommand {
	private String exclude;
	
	public ExcludeCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "exclude", "pattern"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	public Pattern getPattern() {
		return Pattern.compile(".*"+exclude.toLowerCase() + ".*");
	}

}
