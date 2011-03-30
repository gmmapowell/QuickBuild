package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class SetVarCommand extends NoChildCommand implements ConfigApplyCommand {
	protected String name;
	protected String var;
	
	public SetVarCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "name", "property name"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "var", "property value")
		);
	}

	@Override
	public void applyTo(Config config) {
		config.setVarProperty(name, var);
	}

}
