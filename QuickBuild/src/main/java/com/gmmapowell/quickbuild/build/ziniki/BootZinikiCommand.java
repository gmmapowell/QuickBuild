package com.gmmapowell.quickbuild.build.ziniki;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class BootZinikiCommand extends NoChildCommand implements ConfigApplyCommand {

	public BootZinikiCommand(TokenizedLine toks) {
	}


	@Override
	public void applyTo(Config config) {
	}
}
