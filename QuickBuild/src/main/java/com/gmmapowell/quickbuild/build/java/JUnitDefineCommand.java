package com.gmmapowell.quickbuild.build.java;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class JUnitDefineCommand extends NoChildCommand implements ConfigApplyCommand {
	private String name;
	private String value;
	
	public JUnitDefineCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "name", "property to define"), new ArgumentDefinition("*", Cardinality.REQUIRED, "value", "property value"));
	}

	@Override
	public void applyTo(Config config) {
		
	}

	public String getDefine() {
		return "-D"+name+"="+value;
	}

}
