package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class BuildIfCommand extends NoChildCommand implements ConfigApplyCommand {
	private String option;
	private String value;
	private Config config;
	
	public BuildIfCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "option", "build option"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "value", "required option value")
		);
	}

	@Override
	public void applyTo(Config config) {
		this.config = config;
	}

	public boolean isApplicable() {
		if (!config.hasVar(option))
			return false;
		String var = config.getVar(option);
		return var != null && var.equals(value);
	}
}
