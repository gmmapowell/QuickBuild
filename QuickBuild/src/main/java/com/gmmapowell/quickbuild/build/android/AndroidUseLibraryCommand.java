package com.gmmapowell.quickbuild.build.android;

import java.io.File;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class AndroidUseLibraryCommand extends NoChildCommand implements ConfigApplyCommand {
	private String library;
	
	public AndroidUseLibraryCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "library", "library path"));
	}

	@Override
	public void applyTo(Config config) {
		// ? anything?
	}

	public void provideTo(DexBuildCommand dex) {
		dex.addJar(new File(library));
	}

}