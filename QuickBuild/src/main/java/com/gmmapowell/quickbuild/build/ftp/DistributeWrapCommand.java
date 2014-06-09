package com.gmmapowell.quickbuild.build.ftp;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class DistributeWrapCommand extends NoChildCommand implements ConfigApplyCommand {
	private String dir;
	
	public DistributeWrapCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "dir", "wrap directory"));
	}

	@Override
	public void applyTo(Config config) {

	}

	public String getWrap() {
		return dir;
	}
	
	@Override
	public String toString() {
		return "WrapIn["+dir+"]";
	}


}
