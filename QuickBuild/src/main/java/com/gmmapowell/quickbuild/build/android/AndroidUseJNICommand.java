package com.gmmapowell.quickbuild.build.android;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class AndroidUseJNICommand extends NoChildCommand implements ConfigApplyCommand {
	private String usejni;
	private PendingResource pr;
	
	public AndroidUseJNICommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "usejni", "jni jar path"));
	}

	@Override
	public void applyTo(Config config) {
		pr = new PendingResource(usejni);
	}

	public PendingResource getResource() {
		return pr;
	}
}
