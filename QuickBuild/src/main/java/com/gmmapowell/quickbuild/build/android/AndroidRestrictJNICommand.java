package com.gmmapowell.quickbuild.build.android;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class AndroidRestrictJNICommand extends NoChildCommand implements ConfigApplyCommand {
	final List<String> arch = new ArrayList<String>();
	
	public AndroidRestrictJNICommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.ONE_OR_MORE, "arch", "architecture(s)"));
	}

	@Override
	public void applyTo(Config config) {
	}
}
