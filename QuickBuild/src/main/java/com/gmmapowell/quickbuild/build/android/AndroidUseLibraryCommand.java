package com.gmmapowell.quickbuild.build.android;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class AndroidUseLibraryCommand extends NoChildCommand implements ConfigApplyCommand {
	private String library;
	private PendingResource pr;
	
	public AndroidUseLibraryCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "library", "library path"));
	}

	@Override
	public void applyTo(Config config) {
		pr = new PendingResource(library);
	}

	public PendingResource getResource() {
		return pr;
	}
}
