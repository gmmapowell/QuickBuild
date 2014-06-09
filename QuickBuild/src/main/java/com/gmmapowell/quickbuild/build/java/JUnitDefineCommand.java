package com.gmmapowell.quickbuild.build.java;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import org.zinutils.utils.ArgumentDefinition;
import org.zinutils.utils.Cardinality;

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
