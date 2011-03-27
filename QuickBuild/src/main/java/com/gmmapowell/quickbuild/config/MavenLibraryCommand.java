package com.gmmapowell.quickbuild.config;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class MavenLibraryCommand extends NoChildCommand implements ConfigApplyCommand {
	private String pkg;
	
	public MavenLibraryCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "pkg", "maven package name"));
	}

	@Override
	public void applyTo(Config config) {
		config.requireMaven(pkg);
		
	}

}
