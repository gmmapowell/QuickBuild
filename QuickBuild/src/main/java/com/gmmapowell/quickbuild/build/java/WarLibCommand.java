package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class WarLibCommand extends NoChildCommand implements ConfigApplyCommand {
	private String lib;
	
	public WarLibCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "lib", "library"));
	}

	@Override
	public void applyTo(Config config) {
		// TODO Auto-generated method stub
		
	}

	public PendingResource getPendingResource() {
		return new PendingResource(lib);
	}

}
