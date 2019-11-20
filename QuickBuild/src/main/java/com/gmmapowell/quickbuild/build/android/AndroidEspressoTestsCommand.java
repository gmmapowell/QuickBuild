package com.gmmapowell.quickbuild.build.android;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class AndroidEspressoTestsCommand extends NoChildCommand implements ConfigApplyCommand {
	private String project;
	private PendingResource pr;

	public AndroidEspressoTestsCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "project", "APK under test"));
	}

	@Override
	public void applyTo(Config config) {
		pr = new PendingResource(project + ".jar");
	}

	public PendingResource getResource() {
		return pr;
	}
}
