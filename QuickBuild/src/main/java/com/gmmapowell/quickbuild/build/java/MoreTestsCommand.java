package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class MoreTestsCommand extends NoChildCommand implements ConfigApplyCommand {
	private String dir;
	
	public MoreTestsCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "dir", "dir"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	public String getTestDir() {
		return dir;
	}
}
