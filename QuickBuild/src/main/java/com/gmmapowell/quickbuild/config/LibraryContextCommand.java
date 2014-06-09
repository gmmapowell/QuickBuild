package com.gmmapowell.quickbuild.config;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class LibraryContextCommand extends NoChildCommand implements ConfigApplyCommand {
	private String context;
	private String library;

	public LibraryContextCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "context", "context"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "library", "target"));
	}

	@Override
	public void applyTo(Config config) {
		config.bindLibraryContext(context, library);
	}

}
