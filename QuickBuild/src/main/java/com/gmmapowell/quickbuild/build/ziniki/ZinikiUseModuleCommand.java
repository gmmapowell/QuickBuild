package com.gmmapowell.quickbuild.build.ziniki;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class ZinikiUseModuleCommand extends NoChildCommand implements ConfigApplyCommand {
	private String lib;
	private PendingResource resource;

	public ZinikiUseModuleCommand(TokenizedLine toks) {
		toks.process(this,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "lib", "library to reference"));
	}

	@Override
	public void applyTo(Config config) {
		resource = new PendingResource(lib);
	}

	public PendingResource getResource() {
		return resource;
	}
}
