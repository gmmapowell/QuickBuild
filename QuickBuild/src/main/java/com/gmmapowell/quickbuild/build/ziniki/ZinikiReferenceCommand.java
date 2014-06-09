package com.gmmapowell.quickbuild.build.ziniki;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class ZinikiReferenceCommand extends NoChildCommand implements ConfigApplyCommand {
	private String lib;
	private boolean generate;
	private PendingResource resource;

	public ZinikiReferenceCommand(TokenizedLine toks) {
		toks.process(this,
			new ArgumentDefinition("--generate", Cardinality.OPTION, "generate", "choose to generate into proj file"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "lib", "library to reference"));
	}

	@Override
	public void applyTo(Config config) {
		resource = new PendingResource(lib);
	}

	public String getMode() {
		if (generate)
			return null;
		return "--no-generate";
	}
	
	public PendingResource getResource() {
		return resource;
	}
}
