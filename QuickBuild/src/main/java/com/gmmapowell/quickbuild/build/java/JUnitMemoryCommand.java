package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

public class JUnitMemoryCommand extends NoChildCommand implements ConfigApplyCommand {
	private String quant;
	
	public JUnitMemoryCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "quant", "amount of memory to allow"));
	}

	@Override
	public void applyTo(Config config) {
		
	}
	
	public String getMemory() {
		return quant;
	}
}
