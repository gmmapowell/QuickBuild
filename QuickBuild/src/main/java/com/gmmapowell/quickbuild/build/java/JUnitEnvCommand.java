package com.gmmapowell.quickbuild.build.java;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class JUnitEnvCommand extends NoChildCommand implements ConfigApplyCommand {
	private String var;
	private String value;
	
	public JUnitEnvCommand(TokenizedLine toks)
	{
		toks.process(this,
			new ArgumentDefinition("*", Cardinality.REQUIRED, "var", "env var"),
			new ArgumentDefinition("*", Cardinality.REQUIRED, "value", "env value"));
	}

	@Override
	public void applyTo(Config config) {
		while (value.contains("${")) {
			int idx = value.indexOf("${")+2;
			int idx2 = value.indexOf("}", idx);
			if (idx2 == -1)
				throw new UtilException("Malformed reference: " + value + " in definition of " + var);
			String key = value.substring(idx, idx2);
			String var = config.getVar(key);
			value = value.replaceAll("\\$\\{"+key+"\\}", var);
		}
	}

	public String getVar() {
		return var;
	}

	public String getValue() {
		return value;
	}

}
