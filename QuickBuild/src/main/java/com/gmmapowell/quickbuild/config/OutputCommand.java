package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

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
