package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

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
