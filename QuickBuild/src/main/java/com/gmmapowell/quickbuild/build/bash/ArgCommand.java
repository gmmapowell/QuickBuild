package com.gmmapowell.quickbuild.build.bash;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class ArgCommand extends NoChildCommand implements ConfigApplyCommand  {
	private String arg;
	
	public ArgCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED_ALLOW_FLAGS, "arg", "argument"));
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
