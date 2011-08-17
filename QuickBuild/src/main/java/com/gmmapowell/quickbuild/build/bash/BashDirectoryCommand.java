package com.gmmapowell.quickbuild.build.bash;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class BashDirectoryCommand extends NoChildCommand implements ConfigApplyCommand {
	String dir;
	
	public BashDirectoryCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "dir", "directory"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	@Override
	public String toString() {
		return "BashDirectory["+dir+"]";
	}

}
