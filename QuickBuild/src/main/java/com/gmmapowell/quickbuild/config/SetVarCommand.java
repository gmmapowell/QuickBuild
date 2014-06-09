package com.gmmapowell.quickbuild.config;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

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
