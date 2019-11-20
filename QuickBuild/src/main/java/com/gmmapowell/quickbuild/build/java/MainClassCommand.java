package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class MainClassCommand extends NoChildCommand implements ConfigApplyCommand {
	private String clz;
	
	public MainClassCommand(TokenizedLine toks)
	{
		toks.process(this,
				new ArgumentDefinition("*", Cardinality.REQUIRED, "clz", "class"));
	}

	@Override
	public void applyTo(Config config) {
		
	}
	
	public String getName() {
		return clz;
	}

	@Override
	public String toString() {
		return "MainClass[" + clz + "]";
	}

}
