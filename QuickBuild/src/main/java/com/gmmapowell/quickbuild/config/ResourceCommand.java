package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ResourceCommand extends NoChildCommand implements ConfigApplyCommand {
	private String resource;
	private PendingResource pending;
	
	public ResourceCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "resource", "resource"));
	}

	@Override
	public void applyTo(Config config) {
		pending = new PendingResource(resource);
	}

	public PendingResource getPendingResource() {
		return pending;
	}
	
	@Override
	public String toString() {
		return "Resource["+resource+"]";
	}

}
