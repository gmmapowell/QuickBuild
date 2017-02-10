package com.gmmapowell.quickbuild.build.bash;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class BashDirectoryCommand extends NoChildCommand implements ConfigApplyCommand {
	private String dir;
	
	public BashDirectoryCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "dir", "directory"));
	}

	@Override
	public void applyTo(Config config) {
		
	}
	
	public String getDirectory() {
		return dir;
	}

	@Override
	public String toString() {
		return "BashDirectory["+dir+"]";
	}

}
