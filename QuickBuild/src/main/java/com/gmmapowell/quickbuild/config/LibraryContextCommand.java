package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

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
