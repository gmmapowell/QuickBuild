package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class SpecifyTargetCommand extends NoChildCommand implements ConfigApplyCommand {
	private String target;
	
	public SpecifyTargetCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "target", "target"));
	}

	@Override
	public void applyTo(Config config) {
		// TODO Auto-generated method stub
		
	}

	public String getName() {
		return target;
	}

}
