package com.gmmapowell.quickbuild.build.deployment;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class AfterCommand extends NoChildCommand implements ConfigApplyCommand {
	private String resource;
	
	public AfterCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "resource"));
	}

	@Override
	public void applyTo(Config config) {

	}

	public PendingResource getAfter() {
		return new PendingResource(resource);
	}
	
	@Override
	public String toString() {
		return "After["+resource+"]";
	}


}
