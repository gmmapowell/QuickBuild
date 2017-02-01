package com.gmmapowell.quickbuild.build.android;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.quickbuild.core.PendingResource;

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
