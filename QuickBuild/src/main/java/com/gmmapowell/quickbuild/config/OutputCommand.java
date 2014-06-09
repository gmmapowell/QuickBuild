package com.gmmapowell.quickbuild.config;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class OutputCommand extends NoChildCommand implements ConfigApplyCommand {

	private String output;

	public OutputCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "output", "output directory"));
	}

	@Override
	public void applyTo(Config config) {
		config.setOutputDir(output);
	}

}
