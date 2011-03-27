package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.FileUtils;

public class RootCommand extends NoChildCommand implements ConfigApplyCommand {
	private String root;

	public RootCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "root", "root directory"));
		FileUtils.chdir(new File(root));
	}

	@Override
	public void applyTo(Config config) {
	}

}
