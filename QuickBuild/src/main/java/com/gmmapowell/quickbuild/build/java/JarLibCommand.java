package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class JarLibCommand extends NoChildCommand implements ConfigApplyCommand {
	private String lib;
	
	public JarLibCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "lib", "target"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	public PendingResource getResource() {
		return new PendingResource(lib);
	}

}
