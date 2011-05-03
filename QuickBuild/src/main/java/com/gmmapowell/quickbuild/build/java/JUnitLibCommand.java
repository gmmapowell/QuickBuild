package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class JUnitLibCommand extends NoChildCommand implements ConfigApplyCommand {
	private String lib;
	
	public JUnitLibCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "lib", "target"));
	}

	@Override
	public void applyTo(Config config) {
		// TODO Auto-generated method stub
		
	}

	public PendingResource getResource() {
		return new PendingResource(lib);
	}

}
