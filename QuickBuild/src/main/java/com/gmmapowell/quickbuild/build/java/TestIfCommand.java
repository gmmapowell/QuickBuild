package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class TestIfCommand extends NoChildCommand implements ConfigApplyCommand {
	private String option;
	private String value;
	private Config config;
	
	public TestIfCommand(TokenizedLine toks) {
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
