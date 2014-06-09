package com.gmmapowell.quickbuild.config;

import java.io.File;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;
import org.zinutils.utils.FileUtils;

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
