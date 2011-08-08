package com.gmmapowell.quickbuild.build.bash;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ArgCommand extends NoChildCommand implements ConfigApplyCommand  {
	private String arg;
	
	public ArgCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "arg", "argument"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	public String getArg() {
		return arg;
	}
	
	@Override
	public String toString() {
		return "Arg["+arg+"]";
	}


}
