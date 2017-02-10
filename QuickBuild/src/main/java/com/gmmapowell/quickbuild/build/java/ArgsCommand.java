package com.gmmapowell.quickbuild.build.java;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.parser.NoChildCommand;
import org.zinutils.parser.TokenizedLine;

import com.gmmapowell.quickbuild.config.Config;
import com.gmmapowell.quickbuild.config.ConfigApplyCommand;

public class ArgsCommand extends NoChildCommand implements ConfigApplyCommand {
	private List<String> args = new ArrayList<String>();
	
	public ArgsCommand(TokenizedLine toks)
	{
		for (int i=1;i<toks.length();i++)
			args.add(toks.tokens[i]);
	}

	@Override
	public void applyTo(Config config) {
		
	}
	
	public List<String> args() {
		return args;
	}

	@Override
	public String toString() {
		return "Args" + args;
	}
}
