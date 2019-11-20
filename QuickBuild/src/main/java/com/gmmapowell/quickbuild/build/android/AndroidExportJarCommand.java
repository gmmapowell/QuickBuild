package com.gmmapowell.quickbuild.build.android;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class AndroidExportJarCommand extends NoChildCommand implements ConfigApplyCommand {
	
	public AndroidExportJarCommand(TokenizedLine toks) {
	}

	@Override
	public void applyTo(Config config) {
	}
}
