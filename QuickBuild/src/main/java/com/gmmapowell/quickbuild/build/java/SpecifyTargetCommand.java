package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class SpecifyTargetCommand extends NoChildCommand implements ConfigApplyCommand {
	private String target;
	
	public SpecifyTargetCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "target", "target"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	public String getName() {
		return target;
	}

}
