package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ManifestClassPathCommand extends NoChildCommand implements ConfigApplyCommand {
	private String path;
	
	public ManifestClassPathCommand(TokenizedLine toks)
	{
		toks.process(this,
				new ArgumentDefinition("*", Cardinality.REQUIRED, "path", "class path to add to manifest"));
	}

	@Override
	public void applyTo(Config config) {
		
	}
	
	public String getName() {
		return path;
	}

	@Override
	public String toString() {
		return "ManifestClassPath[" + path + "]";
	}

}
