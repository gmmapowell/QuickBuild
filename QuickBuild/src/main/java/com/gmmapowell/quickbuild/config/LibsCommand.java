package com.gmmapowell.quickbuild.config;

import java.io.File;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.java.JavaNature;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class LibsCommand extends NoChildCommand implements ConfigApplyCommand {
	private final File libsDir;
	private String libs;

	public LibsCommand(TokenizedLine toks) {
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "libs", "target"));
		if (libs.equals("/"))
			libsDir = null;
		else
			libsDir = new File(libs);
	}

	@Override
	public void applyTo(Config config) {
		JavaNature n = config.getNature(JavaNature.class);
		if (libsDir == null)
			n.cleanLibDirs();
		else
			n.addLib(libsDir);
	}

}
