package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class IncludePackageCommand extends NoChildCommand implements ConfigApplyCommand {
	private boolean exclude;
	private String pkg;
	
	public IncludePackageCommand(TokenizedLine toks)
	{
		toks.process(this,
				new ArgumentDefinition("-x", Cardinality.OPTION, "exclude", "exclude package"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "pkg", "package"));
	}

	@Override
	public void applyTo(Config config) {
		
	}
	
	public boolean isExclude() {
		return exclude;
	}

	public String getPackage() {
		return pkg;
	}

	@Override
	public String toString() {
		return "IncludePackage[" + pkg +  "]";
	}
}
