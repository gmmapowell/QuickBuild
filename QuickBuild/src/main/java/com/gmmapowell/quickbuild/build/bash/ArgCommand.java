package com.gmmapowell.quickbuild.build.bash;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class ArgCommand extends NoChildCommand implements ConfigApplyCommand  {
	private String arg;
	
	public ArgCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED_ALLOW_FLAGS, "arg", "argument"));
	}

	@Override
	public void applyTo(Config config) {
		while (arg.contains("${")) {
			int idx = arg.indexOf("${")+2;
			int idx2 = arg.indexOf("}", idx);
			if (idx2 == -1)
				throw new UtilException("Malformed reference: " + arg);
			String key = arg.substring(idx, idx2);
			String var = config.getVar(key);
			arg = arg.replaceAll("\\$\\{"+key+"\\}", var);
		}
	}

	public String getArg() {
		return arg;
	}
	
	@Override
	public String toString() {
		return "Arg["+arg+"]";
	}
}
