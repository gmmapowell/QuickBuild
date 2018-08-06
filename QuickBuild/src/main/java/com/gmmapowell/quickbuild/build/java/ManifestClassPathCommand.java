package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

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
